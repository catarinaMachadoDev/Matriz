'use strict';
const app=document.querySelector('#app'),identity=document.querySelector('#identity');
const state={user:null,csrf:null,search:'',status:'',priority:'',offset:0,projectOffset:0};
const esc=x=>String(x??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const label=x=>({todo:'To do',in_progress:'In progress',done:'Done',low:'Low',medium:'Medium',high:'High'}[x]||x);
const badge=x=>`<span class="badge ${esc(x)}">${esc(label(x))}</span>`;
const options=(values,current)=>values.map(v=>`<option value="${esc(v)}" ${v===current?'selected':''}>${esc(label(v))}</option>`).join('');
const field=(name,title,value='',max=120,required=true,area=false)=>`<label for="${name}">${title}${required?' *':''}</label><${area?'textarea':'input'} id="${name}" name="${name}" maxlength="${max}" ${required?'required':''}${area?'':` value="${esc(value)}"`}>${area?esc(value)+'</textarea>':''}`;
function notice(text){const n=document.querySelector('#notice');n.textContent=text;setTimeout(()=>{if(n.textContent===text)n.textContent='';},5000);}
async function token(){const r=await fetch('/api/auth/csrf',{credentials:'same-origin'});if(!r.ok)throw Error('Unable to establish a session.');state.csrf=(await r.json()).token;}
async function renewToken(path){
  if(path!='/auth/login'&&state.user){
    const current=await api('/auth/me');
    if(current.id!==state.user.id)throw Error('The signed-in account changed. Reload before continuing.');
  }
  await token();
}
async function api(path,method='GET',body,retryCsrf=true){
  if(method!=='GET'&&!state.csrf)await renewToken(path);
  const r=await fetch('/api'+path,{method,credentials:'same-origin',headers:{'Content-Type':'application/json',...(method==='GET'?{}:{'X-CSRF-Token':state.csrf})},...(body===undefined?{}:{body:JSON.stringify(body)})});
  if(r.status===204)return null;
  let data;
  try{data=await r.json();}catch{data={message:'The server could not complete the request.'};}
  if(r.status===403&&data.code==='CSRF_INVALID'&&retryCsrf){
    state.csrf=null;
    await renewToken(path);
    return api(path,method,body,false);
  }
  if(!r.ok){
    if(r.status===401&&path!='/auth/login')state.csrf=null;
    const error=Error(data.message||'Request failed.');
    error.status=r.status;
    error.fields=data.fields||{};
    throw error;
  }
  return data;
}
function login(){identity.innerHTML='';app.innerHTML=`<section class="login"><div class="eyebrow">Your team, in focus</div><h1>Welcome to Matriz.</h1><p class="muted">A little clarity for everything your team is building.</p><form class="card" data-form="login"><h2>Sign in</h2><label for="username">Username</label><input id="username" name="username" required maxlength="80" autocomplete="username"><label for="password">Password</label><input id="password" name="password" type="password" required maxlength="200" autocomplete="current-password"><p class="error" role="alert"></p><button>Sign in to your workspace</button></form></section>`;}
function counts(c){return `<div class="stats"><div class="stat"><small>Total tasks</small><strong>${c.total}</strong></div><div class="stat"><small>To do</small><strong>${c.todo}</strong></div><div class="stat"><small>In progress</small><strong>${c.in_progress}</strong></div><div class="stat"><small>Done</small><strong>${c.done}</strong></div></div><p class="hint">${c.total?c.percentage+'% complete':'No tasks'} · All tasks in this project</p><progress max="100" value="${c.percentage}">${c.percentage}%</progress>`;}
function pagination(page,kind){return `<div class="pagination"><span>${page.total?state[kind]+1:0}–${Math.min(state[kind]+page.items.length,page.total)} of ${page.total}</span><div><button class="secondary" data-page="${kind}" data-delta="-20" ${state[kind]===0?'disabled':''}>Previous</button> <button class="secondary" data-page="${kind}" data-delta="20" ${state[kind]+20>=page.total?'disabled':''}>Next</button></div></div>`;}
async function dashboard(seq){const data=await loadPage('/projects?search='+encodeURIComponent(state.search),'projectOffset',seq);if(seq!==rendering)return;view={};app.innerHTML=`<div class="heading"><div class="eyebrow">Workspace overview</div><h1>Good work starts here.</h1></div><div class="grid"><section><form class="toolbar" data-form="search"><label for="search">Find a project</label><input id="search" name="search" placeholder="Search by project name" value="${esc(state.search)}" maxlength="100"><button class="secondary">Search</button></form><div class="cards">${data.items.map(p=>`<a class="card project" href="#/projects/${p.id}"><div class="eyebrow">${p.managerId===state.user.id?'Managing':'Team project'}</div><h2>${esc(p.name)}</h2><p class="muted">${esc(p.description)}</p><div class="badges">${badge('todo')} ${p.counts.todo} ${badge('in_progress')} ${p.counts.in_progress} ${badge('done')} ${p.counts.done}</div><p class="hint">${p.counts.total?p.counts.percentage+'% complete':'No tasks yet'}</p><progress max="100" value="${p.counts.percentage}"></progress></a>`).join('')||'<p class="empty">No projects found. Create one to get started.</p>'}</div>${pagination(data,'projectOffset')}</section><aside><form class="card" data-form="project-create"><h2>Create a project</h2><p class="muted">Give your team a shared place to make progress.</p>${field('name','Project name','',100)}${field('description','Description','',1000,false,true)}<p class="error" role="alert"></p><div class="actions"><button>Create project</button></div></form></aside></div>`;}
async function project(id,seq){const [p,tasks,members]=await Promise.all([api('/projects/'+id),loadPage(`/projects/${id}/tasks?status=${state.status}&priority=${state.priority}`,'offset',seq),api(`/projects/${id}/members`)]);if(seq!==rendering)return;let eligible=[];if(p.managerId===state.user.id)eligible=await api(`/projects/${id}/eligible-users`);if(seq!==rendering)return;view={project:p};app.innerHTML=`<div class="crumb"><a href="#/">All projects</a> / ${esc(p.name)}</div><div class="heading"><div><div class="eyebrow">Project workspace</div><h1>${esc(p.name)}</h1><p class="muted preserve">${esc(p.description)}</p></div></div><section class="card">${counts(p.counts)}</section><div class="grid"><section><div class="card"><h2>Tasks</h2><form class="toolbar" data-form="filters"><label for="statusFilter">Status</label><select name="status" id="statusFilter"><option value="">All statuses</option>${options(['todo','in_progress','done'],state.status)}</select><label for="priorityFilter">Priority</label><select name="priority" id="priorityFilter"><option value="">All priorities</option>${options(['low','medium','high'],state.priority)}</select><button class="secondary">Apply filters</button></form><ul class="list">${tasks.items.map(t=>`<li class="task-row"><div><a href="#/projects/${id}/tasks/${t.id}">${esc(t.title)}</a><small>${t.assignees.map(a=>esc(a.displayName)).join(', ')||'Unassigned'}</small></div><div class="badges">${badge(t.status)}${badge(t.priority)}</div></li>`).join('')||'<li class="empty">No tasks match this view.</li>'}</ul>${pagination(tasks,'offset')}</div><div class="card"><h2>People</h2>${members.map(u=>`<p>${esc(u.displayName)} <small class="muted">${u.id===p.managerId?'Project manager':'Member'}</small></p>`).join('')}${p.managerId===state.user.id?`<form data-form="member"><label for="userId">Add an existing user</label><select name="userId" id="userId">${eligible.filter(u=>!members.some(m=>m.id===u.id)).map(u=>`<option value="${u.id}">${esc(u.displayName)}</option>`).join('')}</select><p class="error" role="alert"></p><div class="actions"><button ${eligible.length===members.length?'disabled':''}>Add member</button></div></form>`:''}</div></section><aside><form class="card" data-form="task-create"><h2>Create task</h2>${field('title','Title')}${field('description','Description','',2000,true,true)}<p class="hint">Starts as To do, medium priority, with no assignees.</p><p class="error" role="alert"></p><div class="actions"><button>Create task</button></div></form>${p.managerId===state.user.id?`<details class="card"><summary>Project settings</summary><form data-form="project-edit">${field('editName','Project name',p.name,100)}${field('editDescription','Description',p.description,1000,false,true)}<p class="error" role="alert"></p><div class="actions"><button>Save project</button><button type="button" class="danger" data-action="delete-project">Delete project</button></div></form></details>`:''}</aside></div>`;}
async function task(projectId,taskId,seq,preserveDraft=false){const [t,members,comments,p]=await Promise.all([api(`/projects/${projectId}/tasks/${taskId}`),api(`/projects/${projectId}/members`),api(`/projects/${projectId}/tasks/${taskId}/comments`),api('/projects/'+projectId)]);if(seq!==rendering)return;const draft=preserveDraft?taskDraft():null;view={project:p,task:t};app.innerHTML=`<div class="crumb"><a href="#/">All projects</a> / <a href="#/projects/${projectId}">${esc(p.name)}</a> / Task</div><div class="heading"><div><div class="eyebrow">Task detail</div><h1>${esc(t.title)}</h1><div class="badges">${badge(t.status)}${badge(t.priority)}</div></div></div><div class="grid"><section><form class="card" data-form="task-edit"><h2>Task details</h2>${field('title','Title',t.title)}${field('description','Description',t.description,2000,true,true)}<div class="split"><div><label for="status">Status</label><select name="status" id="status">${options(['todo','in_progress','done'],t.status)}</select></div><div><label for="priority">Priority</label><select name="priority" id="priority">${options(['low','medium','high'],t.priority)}</select></div></div><p class="error" role="alert"></p><div class="actions"><button>Save changes</button><button type="button" class="danger" data-action="delete-task">Delete task</button></div></form><section class="card"><h2>Conversation</h2>${comments.map(c=>`<article class="comment"><strong>${esc(c.authorName)}</strong> <small>${esc(new Date(c.createdAt).toLocaleString())}</small><p>${esc(c.content)}</p></article>`).join('')||'<p class="muted">No comments yet. Start the conversation.</p>'}<form data-form="comment">${field('content','Add a comment','',2000,true,true)}<p class="error" role="alert"></p><div class="actions"><button>Post comment</button></div></form></section></section><aside><div class="card"><h2>Assignees</h2>${t.assignees.map(a=>`<div class="pill"><span>${esc(a.displayName)}</span><button class="link" data-action="unassign" data-user="${a.id}" aria-label="Remove ${esc(a.displayName)}">Remove</button></div>`).join('')||'<p class="muted">No one assigned yet.</p>'}<form data-form="assign"><label for="assignee">Add a project member</label><select name="userId" id="assignee">${members.filter(m=>!t.assignees.some(a=>a.id===m.id)).map(m=>`<option value="${m.id}">${esc(m.displayName)}</option>`).join('')}</select><p class="error" role="alert"></p><div class="actions"><button ${members.length===t.assignees.length?'disabled':''}>Assign member</button></div></form></div><div class="card"><h3>Keep everyone in sync</h3><p class="muted">Save your changes before navigating away. If someone else edits this task first, Matriz will ask you to refresh.</p></div></aside></div>`;
  if(draft){
    app.querySelector("[data-form=task-edit]").replaceWith(draft.form);
    view.task={...t,version:draft.version};
  }
}
let rendering=0;
let view={};
async function loadPage(path,key,seq){
  const fetchPage=offset=>api(path+'&limit=20&offset='+offset);
  let page=await fetchPage(state[key]);
  if(seq!==rendering)return;
  const lastOffset=Math.floor(Math.max(0,page.total-1)/20)*20;
  if(page.offset>lastOffset){
    state[key]=lastOffset;
    page=await fetchPage(lastOffset);
  }
  return page;
}
function taskDraft(){
  const form=app.querySelector('[data-form=task-edit]');
  if(!form||!view.task)return null;
  const values=Object.fromEntries(new FormData(form));
  const {title,description,status,priority,version}=view.task;
  const saved={title,description,status,priority};
  return Object.keys(saved).some(key=>values[key]!==saved[key])?{form,version}:null;
}
async function refreshTaskWithDraft(){
  const {project,task:current}=view;
  await task(project.id,current.id,++rendering,true);
}
function showCommentRefreshError(error,target){
  if(error.status===401){actionError(error,target);return;}
  const retry=document.createElement('button');
  retry.type='button';
  retry.textContent='Retry refresh';
  retry.addEventListener('click',async()=>{
    retry.disabled=true;
    try{await refreshTaskWithDraft();}
    catch(refreshError){showCommentRefreshError(refreshError,target);}
  });
  target.replaceChildren('Comment saved. Unable to refresh the conversation. ',retry);
}
function actionError(error,target){
  if(error.status===401){
    const link=document.createElement('a');
    link.href='/';link.target='_blank';link.rel='noopener';link.textContent='Sign in in a new tab';
    const message=target||document.querySelector('#notice');
    message.textContent='Your session expired. ';
    message.append(link,', then retry this action.');
    return;
  }
  if(target)target.textContent=error.message+Object.entries(error.fields||{}).map(([key,value])=>'\n'+key+': '+value).join('');
  else notice(error.message);
}
async function render(){const seq=++rendering;view={};if(!state.user){login();return;}identity.innerHTML=`<span>${esc(state.user.displayName)}</span><button class="secondary" data-action="logout">Sign out</button>`;app.innerHTML='<p class="empty">Loading your workspace…</p>';try{const parts=location.hash.replace(/^#\/?/,'').split('/').filter(Boolean);if(parts[0]==='projects'&&parts[1]){if(parts[2]==='tasks'&&parts[3])await task(parts[1],parts[3],seq);else await project(parts[1],seq);}else await dashboard(seq);}catch(e){if(seq!==rendering)return;if(e.status===401){state.user=null;login();return;}app.innerHTML=`<section class="card"><h2>Unable to load this view</h2><p class="error">${esc(e.message)}</p><a class="button" href="#/">Back to projects</a> <button class="secondary" data-action="refresh">Try again</button></section>`;}}
document.addEventListener('submit',async event=>{const form=event.target.closest('[data-form]');if(!form)return;event.preventDefault();const data=Object.fromEntries(new FormData(form)),kind=form.dataset.form;const buttons=[...form.querySelectorAll('button')];buttons.forEach(b=>b.disabled=true);const error=form.querySelector('.error');if(error)error.textContent='';try{const seq=rendering;const {project,task}=view;const base=project?'/projects/'+project.id:'';const taskPath=task?base+'/tasks/'+task.id:'';
 switch(kind){case 'login':await token();state.user=await api('/auth/login','POST',data);await token();await render();return;
 case 'search':state.search=data.search;state.projectOffset=0;break;
 case 'filters':state.status=data.status;state.priority=data.priority;state.offset=0;break;
 case 'project-create':{const p=await api('/projects','POST',data);notice('Project created.');if(seq===rendering)location.hash='#/projects/'+p.id;return;}
 case 'project-edit':await api(base,'PUT',{name:data.editName,description:data.editDescription,version:project.version});notice('Project updated.');break;
 case 'member':await api(base+'/members','POST',data);notice('Member added.');break;
 case 'task-create':
  await api(base+'/tasks','POST',data);
  form.reset();
  notice('Task created.');
  if(seq===rendering){
    state.status='';state.priority='';state.offset=0;
    await render();
  }
  return;
 case 'task-edit':await api(taskPath,'PUT',{...data,version:task.version});notice('Task updated.');break;
 case 'assign':await api(taskPath+'/assignees','POST',data);notice('Member assigned.');if(seq===rendering)await refreshTaskWithDraft();return;
 case 'comment':
  form.elements.content.readOnly=true;
  try{
    await api(taskPath+'/comments','POST',data);
    form.reset();
    notice('Comment posted.');
    if(seq===rendering){
      try{await refreshTaskWithDraft();}
      catch(refreshError){showCommentRefreshError(refreshError,error);}
    }
  }finally{form.elements.content.readOnly=false;}
  return;
 }if(seq===rendering)await render();
 }catch(e){if(kind==='login'&&error)error.textContent=e.message;else actionError(e,error);}finally{buttons.forEach(b=>b.disabled=false);}});
document.addEventListener('click',async event=>{const b=event.target.closest('button[data-action],button[data-page]');if(!b)return;b.disabled=true;try{if(b.dataset.page){state[b.dataset.page]=Math.max(0,state[b.dataset.page]+Number(b.dataset.delta));await render();return;}const seq=rendering;const {project,task}=view;const base=project?'/projects/'+project.id:'';switch(b.dataset.action){case 'logout':
  try{
    await api('/auth/logout','POST');
  }catch(error){
    if(error.status!==401)throw error;
  }
  state.user=null;state.csrf=null;
  document.querySelector('#notice').textContent='';
  location.hash='#/';
  await render();
  break;case 'refresh':await render();break;case 'delete-project':if(confirm('Delete this project and ALL its tasks, comments, assignments and memberships? This cannot be undone.')){await api(base+'?version='+project.version,'DELETE');notice('Project deleted.');if(seq===rendering)location.hash='#/';}break;case 'delete-task':if(confirm('Delete this task and all its comments and assignments? This cannot be undone.')){await api(base+'/tasks/'+task.id+'?version='+task.version,'DELETE');notice('Task deleted.');if(seq===rendering)location.hash='#'+base;}break;case 'unassign':await api(base+'/tasks/'+task.id+'/assignees/'+b.dataset.user,'DELETE');notice('Assignment removed.');if(seq===rendering)await refreshTaskWithDraft();break;}}catch(e){actionError(e);}finally{b.disabled=false;}});
window.addEventListener('hashchange',()=>{state.offset=0;render();});
(async()=>{try{await token();state.user=await api('/auth/me');}catch(e){if(e.status!==401)notice(e.message);}await render();})();
