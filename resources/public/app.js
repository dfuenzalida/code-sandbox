// The list of tasks is a global array
var tasks = [];

// The ID of the interval function used to retrieve the user's tasks
var tasksInterval = undefined;

// The access token
var token = undefined;

// show an element by its ID
function show(id) {
  document.getElementById(id).style.display = "";
}

// hide an element by its ID
function hide(id) {
  document.getElementById(id).style.display = "none";
}

// return the value property of an object by its ID
function valueOf(id) {
  return document.getElementById(id).value;
}

// Replace the children of an element (by its ID) with the given HTML fragment
function updateById(id, html) {
  document.getElementById(id).innerHTML = html;
}

// show the task detail form
function showTaskDetail() {
  hide('taskCreateForm');
  show('taskDetails');
}

// show the task creation form
function showCreateForm() {
  hide('taskDetails');
  show('taskCreateForm');
}

// fetch the tasks data from the backend and update the contents of the task list
function getTasks() {
  fetch('/api/tasks', {
    method: 'GET',
    headers: { 'Authorization': 'Bearer ' + token }
  }).then(response => response.json())
    .then(data => {
      tasks = data.tasks;
      var html = tasks.map(task => renderTask(task)).join("");
      updateById('taskListContainer', html);
    });
}

// Given a task object, return HTML markup of one task in the list of tasks
function renderTask(task) {
  return '' +
    '<a href="#" onClick="taskDetail(' + task.id + ')" ' +
    ' class="list-group-item list-group-item-action">' +
    '<div class="d-flex w-100 justify-content-between">' +
    '<h5 class="mb-1">' + (task.name || '<i>no name</i>') + '</h5>' +
    '<small>' + task.state + '</small>' +
    '</div><p class="mb-1">' + task.id + '</p></a>';
}

// Escape a single character into its HTML entity, if any of '&', '<', '>'
function escapeChar(c) {
  if (c === "&") return "&amp;";
  if (c === "<") return "&lt;";
  if (c === ">") return "&gt;";
  else return c;
}

// Escape all entities in a given string
function escapeString(s) {
  return s.toString().split("").map(escapeChar).join("");
}

// show a table with the properties of a task found by its ID
function taskDetail(taskId) {
  showTaskDetail();
  const task = tasks.find(t => t.id === taskId);
  var html = '<table class="table table-bordered table-striped table-condensed table-hover"><tbody>';

  Object.keys(task).map(k => {
    var v = escapeString(task[k]);
    html += '<tr><th scope="row">' + k + '</th><td>';
    html += (k === "code" || k === "stdout" || k === "stderr") ? ('<pre>' + v + '</pre>') : v;
    html += '</td></tr>';
  });

  html += '</tbody></table>';
  updateById('taskDetailsTable', html);
}

// generic POST to a given URL with a body of the JSON-serialized version of the data param
async function postData(url = '', data = {}) {
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Authorization': 'Bearer ' + token,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(data)
  });
  return response.json();
}

// Show an alert in the task creation form
function formAlert(text) {
  updateById('alert', text);
  show('alert');
  setTimeout(() => hide('alert'), 2000);
}

// Submit handler for the task creation form
function submitForm() {
  const taskData = {
    lang: valueOf('scriptLang'),
    name: valueOf('scriptName'),
    code: valueOf('scriptCode')
  };
  postData('/api/tasks', taskData).then(response => {
    formAlert('Task #' + response.id + ' created');
    console.log(JSON.stringify(response));
    document.getElementsByTagName('form')[0].reset();
  });
}

// Initialize the UI
function init() {
  showCreateForm();
  getTasks();
  tasksInterval = setInterval(getTasks, 3000);
}

// login form handler
function login() {
  const loginData = {
    username: valueOf('username'),
    password: valueOf('userpassword')
  };
  postData('/api/tokens', loginData).then(response => {
    console.log(JSON.stringify(response));
    if (response.error) {
      formAlert(response.cause);
    } else {
      token = response.token;
      hide('login-form');
      show('main-ui');
      init();
    }
  });
}

hide('main-ui');
