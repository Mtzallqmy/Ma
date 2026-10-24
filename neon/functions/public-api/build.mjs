import {build} from 'esbuild';
await build({entryPoints:['panel/client.js'],bundle:true,platform:'browser',format:'esm',minify:true,outfile:'panel/panel.js'});
await build({entryPoints:['index.js'],bundle:true,platform:'node',target:'node24',format:'esm',minify:true,outfile:'dist/index.mjs'});
await build({entryPoints:['academy-admin.js'],bundle:true,platform:'node',target:'node24',format:'esm',minify:true,loader:{'.html':'text','.css':'text'},plugins:[{name:'panel-js-text',setup(b){b.onLoad({filter:/panel\/panel\.js$/},async a=>({contents:await(await import('node:fs/promises')).readFile(a.path,'utf8'),loader:'text'}));}}],outfile:'dist/academy-admin.mjs'});
