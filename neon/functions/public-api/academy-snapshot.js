export async function academySnapshot(query) {
  const load = async sql => {
    const rows=await query(sql+' LIMIT 1001');
    if(rows.length>1000)throw new Error('Academy catalog requires pagination');
    return rows;
  };
  const lessons=(await load(`SELECT l.published_body AS payload FROM academy.lessons l JOIN academy.modules m ON m.id=l.module_id JOIN academy.courses c ON c.id=m.course_id WHERE c.is_public AND l.published_revision IS NOT NULL AND l.published_body IS NOT NULL ORDER BY l.id`)).map(x=>x.payload);
  const courses=(await load('SELECT payload FROM academy.courses WHERE is_public ORDER BY id')).map(x=>x.payload).filter(c=>lessons.some(l=>l.courseId===c.id));
  const used=new Set(lessons.flatMap(l=>l.sourceIds));
  const sources=(await load('SELECT payload FROM academy.sources ORDER BY id')).map(x=>x.payload).filter(s=>used.has(s.id));
  return {schemaVersion:1,courses,lessons,sources};
}
