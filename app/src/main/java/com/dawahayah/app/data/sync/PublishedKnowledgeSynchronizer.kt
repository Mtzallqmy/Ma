package com.dawahayah.app.data.sync

import android.content.Context
import androidx.room.withTransaction
import com.dawahayah.app.core.errors.*
import com.dawahayah.app.data.local.*
import com.dawahayah.app.data.remote.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class CloudOwnedIds(
    val systems: List<String> = emptyList(), val organs: List<String> = emptyList(),
    val diseases: List<String> = emptyList(), val ingredients: List<String> = emptyList()
)

/** A failed/incomplete response can never clear the local library or user records. */
class PublishedKnowledgeSynchronizer(
    private val context: Context,
    private val database: DawaHayahDatabase,
    private val remote: RemoteContentDataSource,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    suspend fun refresh(): AppResult<Int> = when (val result = remote.fetchPublishedSnapshot()) {
        is AppResult.Error -> result
        is AppResult.Success -> applySnapshot(result.data)
    }

    suspend fun applySnapshot(snapshot: PublishedSnapshot): AppResult<Int> {
        val data = snapshot.bundle
        val ids = CloudOwnedIds(data.systems.map { it.id }, data.organs.map { it.id },
            data.diseases.map { it.id }, data.activeIngredients.map { it.id })
        val groups = listOf(ids.systems, ids.organs, ids.diseases, ids.ingredients)
        if (snapshot.schemaVersion != 1 || data.schemaVersion != 1 || !snapshot.complete ||
            !snapshot.revision.matches(Regex("[0-9a-f]{64}")) || groups.any { list ->
                list.size > 10000 || list.distinct().size != list.size ||
                    list.any { !it.matches(Regex("[A-Za-z0-9._:-]{1,128}")) }
            } || data.organs.any { it.systemId !in ids.systems } ||
            data.organs.sumOf { it.structures.size + it.sections.size + it.figures.size } > 10000 ||
            data.diseases.sumOf { it.sections.size } > 10000
        ) return AppResult.Error(AppError.GeneralFailure("تحديث غير مكتمل أو غير متوافق؛ احتُفظ بالمحتوى المحلي.",
            "Incomplete or incompatible snapshot; local content retained."))
        return try {
            database.withTransaction {
                val dao = database.knowledgeDao()
                val encoded = json.encodeToString(snapshot)
                if (dao.getSyncValue("snapshot") != encoded) {
                    val old = dao.getSyncValue("cloud_owned_ids")?.let { json.decodeFromString<CloudOwnedIds>(it) } ?: CloudOwnedIds()
                    val replaceOrgans = (old.organs + ids.organs).distinct()
                    val replaceDiseases = (old.diseases + ids.diseases).distinct()
                    dao.deleteCloudStructures(replaceOrgans); dao.deleteCloudLabels(replaceOrgans)
                    dao.deleteCloudOrganSections(replaceOrgans); dao.deleteCloudFigures(replaceOrgans)
                    dao.deleteCloudOrgans(replaceOrgans); dao.deleteCloudSystems((old.systems + ids.systems).distinct())
                    dao.deleteCloudDiseaseSections(replaceDiseases); dao.deleteCloudDiseaseSystemLinks(replaceDiseases)
                    dao.deleteCloudDiseaseOrganLinks(replaceDiseases); dao.deleteCloudAttributions(replaceDiseases)
                    dao.deleteCloudDiseases(replaceDiseases); dao.deleteCloudIngredients((old.ingredients + ids.ingredients).distinct())
                    SeedDatabaseInitializer(context, dao, json).persistBundle(data, fromCloud = true)
                    dao.putSyncValue(SyncStateEntity("cloud_owned_ids", json.encodeToString(ids)))
                    dao.putSyncValue(SyncStateEntity("snapshot", encoded))
                }
                AppResult.Success(groups.sumOf { it.size })
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { AppResult.Error(AppError.GeneralFailure(
            "تعذر حفظ التحديث؛ لم تُغيّر النسخة المحلية.", "Could not commit update; local snapshot retained.")) }
    }
}
