# code-sandbox

## Description

CodeSandbox is a RESTful wrapper around a Docker container that runs the Groovy interpreter with code submitted by users.

Running the code in a container allows simpler management of system resources and handling security: the code submitted by the user(s) runs in a sandbox, minimizing the risk of compromising the host service and allowing simpler definition of limits to the execution of those tasks.

## Requisites

* Docker and the `groovy` image from Docker Hub
* Java 8 JDK or newer (JDK 11 or newer recommended)
* Leiningen

## First time setup

* Install the Java 11 JDK and Docker in Ubuntu 20.04, you can run the following:

```
$ sudo apt-get install openjdk-11-jdk docker.io
```

* In order to run Docker (at least in Linux), you'll need to add your current user to the `docker` group:

```
$ sudo adduser $USER docker
```

You'll need to log out and log back in for the new group membership to be reflected.

* Download the `groovy` container image and validate that it runs:

```
$ docker run -it --rm groovy groovy --version
[... output elided ...]
Groovy Version: 3.0.7 JVM: 1.8.0_275 Vendor: AdoptOpenJDK OS: Linux
```

Note that in the output above, the Java version reported is `1.8.0_275`. This is fine because it's the *Java runtime installed in the container*. The service (CodeSandbox) will run on a different JDK.

## Running locally

* You can run project from the command line with `lein run`
* Once the service is up and running, it will be listening for requests on port 3000 by default. You can submit task requests using tools like Postman or cURL (see examples below), or use the browser-based UI by opening the following URL: http://localhost:3000/ (enter the user `demo@example.com` with no password)

## Packaging

You can create the JAR file with `lein uberjar`, which will build and package the application as a single Java JAR file that contains everything needed to run the application (provided that you have the JDK 11 installed).

This JAR file can be run directly with `java -jar target/uberjar/sandbox.jar`. The first time you'll need to define the environment variable `DATABASE_URL` with a JDBC URL (like `jdbc:h2:./sandbox_dev.db`) and run `java -jar target/uberjar/sandbox.jar migrate` to create the tables. See https://luminusweb.com/docs/migrations.html for details.

## Service Design and architecture

The service is intentionally simple: a RESTful wrapper around a Task Runner that receives requests containing code and passing the code to executors that will store the code in a temporary file and pass it to docker instances that will run the code.

The criteria for this approach was that running arbitrary code submitted by users on a shared service is difficult to do securely, so the emphasis was on delivering a service that would allow users to submit their requests and have code running (within time limits) for a number of jobs at a time (configurable) and tackle other concerns afterwards.

### Task schema

Tasks are internally represented as objects with the following properties:

* `id` - a numeric identifier of this instance of the task
* `name` - a human-readable name for this task
* `state` - a human-readable identifier of the execution state of this task. Can take one of the following values: `CREATED`, `QUEUED`, `RUNNING`, `COMPLETE`. Note that tasks fail to run at all (invalid code, exceptions, etc.) will also reach the `COMPLETE` state.
* `lang` - the identifier of the programming language used for the code of this task. Possible values include `groovy`, `perl` and `echo` (which can be used for debugging). `echo` just repeats the name of the temporary file used to store the code.
* `code` - the source code of the task. Ideally, it should be a valid program in the language selected in the `lang` property.
* `stout` - the contents of the *standard output* of the program are copied to this property once the task has fully run.
* `sterr` - the contents of the *standard error* of the program are copied to this property once the task has fully run.
* `exitCode` - the exit code of executing the code is copied to this value. Non-zero values usually signal that the program ended abnormally (runtime exceptions, errors, timeout, etc.)

* `createdDate` - an ISO 8601 timestamp when the task was created in the service
* `startedDate` - an ISO 8601 timestamp when the task started executing in the service
* `endDate` - an ISO 8601 timestamp when the task finished executing in the service

### API endpoints

The following is the list of the APIs implemented in this version of the service. The POST request expects a JSON object in the body of the request, they return JSON responses.


* `GET /api/tasks` - Added mostly as a convenience for the browser-based UI, this API returns an array with all the tasks submitted to the service

* `POST /api/tasks` - Submits a new task request. Expects a JSON object with the following properties:
    * `name` - (Optional) human-readable name for this task (optional)
    * `lang` - (Required) the programming language used to write the code of the task, by default it needs to be one of `groovy`, `echo` or `perl`.
    * `code` - (Required) the actual code submitted for execution. Note that the task runs in an environment where no extra libraries are provided beyond the core libraries of Java, Groovy and Perl.
  
* `GET /api/tasks/<taskId>` - Given a valid `taskId`, returns a single JSON object with the details of the task.

### Configuration

TBC

### Limitations, concerns and possible enhancements

* **Security**: The service does not implement any mechanism of **authentication and authorization** - if a request meets the bare minimum validation, a task is queued and (eventually) executed. It is trivial an unlimited amount of "slow" tasks that simply wait and prevent other users to submit tasks. An enhancement on this would be to include:
    * Authentication and authorization - only valid users of an organization should be allowed to see *their own* tasks and submit tasks. For RESTful services is very common to provide an endpoint where authorized users or apps submit a POST request to create a *token* that is sent with the HTTP headers and used by the service to validate the request.
    * An usage rate/quota system: for a given principal or account there should be a limit on the number of tasks they can submit per unit of time (eg. "5 tasks per minute") and/or runtime (eg. "100 seconds of runtime per minute, per client, across all their tasks")

* **Breaking out the container**: CodeSandbox was written with the assumption that containers are generally safe but it could be possible to [run scripts that run shell commands](https://stackoverflow.com/a/159270/483566) and extract precise version information of the environment and then attempt to recreate [known exploits for Docker](https://cve.mitre.org/cgi-bin/cvekey.cgi?keyword=docker), like the following:

```groovy
def sout = new StringBuilder(), serr = new StringBuilder()
def proc = 'uname -r -v'.execute() // prints the Kernel version
proc.consumeProcessOutput(sout, serr)
proc.waitForOrKill(1000)
println "OUT:$sout\n\nERR:$err"
```

... running the latest version of the container and using current, patched version of the JDK and Docker helps to prevent this scenario

* **Changing the Database**:
    * Running multiple instances of the service would require some changes: each instance would have to share some shared storage for the task information. The current migration files were written for the H2 database.

* **Scaling the service**:
    * The service launches containers but would need some work to be *containerized* itself. There are some options to implement something like "docker-in-docker" like the `docker.sock` approach on this post: https://devopscube.com/run-docker-in-docker/. With some extra effort, you could containerize CodeSandbox and be able to use tools like Docker compose or Kubernetes to launch as many instances as needed.

## Example usage

In the example below, we send a POST request to the `/api/tokens` endpoint and extract the token out of the response, saving it into the `APITOKEN` environment variable:

```
export APITOKEN=`curl --no-progress-meter -X POST http://localhost:8080/api/tokens -H "Content-type: application/json" -d '{"username":"demo@example.com", "password":"SECRET!"}' | awk -F'"' '{ print $4 }'`

echo $APITOKEN
```

In the calls to the other endpoints, we'll need to provide the token as part of the `Authorization` header, like in the following calls to create a new task and then retrieving all the tasks for the user associated with the token:

```
curl --no-progress-meter -X POST http://localhost:8080/api/tasks -H "Content-type: application/json" -H "Authorization: Bearer $APITOKEN" -d '{"name":"token task", "lang":"groovy", "code":"println(1)"}' | json_pp

curl --no-progress-meter http://localhost:8080/api/tasks -H "Content-type: application/json" -H "Authorization: Bearer $APITOKEN" | json_pp
```

### Example tasks

The following programs can be used to test the service:

---

A Groovy program that prints the result of `(2*3*4)`:

```
$ curl --no-progress-meter -X POST localhost:8080/api/tasks -H 'Content-type:application/json' -H "Authorization: Bearer $APITOKEN" -d '{"name": "example", "lang": "groovy", "code": "println(2*3*4)"}' | json_pp
```

returns something like the following:

```javascript
{
   "code" : "println(2*3*4)",
   "createdDate" : "2020-12-12T08:34:09.160+00:00",
   "endDate" : "2020-12-12T08:34:10.995+00:00",
   "exitCode" : 0,
   "id" : 10,
   "lang" : "groovy",
   "name" : "example",
   "startedDate" : "2020-12-12T08:34:09.162+00:00",
   "state" : "COMPLETE",
   "stderr" : "",
   "stdout" : "24"
}
```

---

**Creating a task that won't execute completely due to timeout**

Launch the service and open the Web UI at http://localhost:8080/ and log in with any of the users defined in `LoadDatabase.java`. Note that the user name is required but any password will do.

You can submit the following Groovy program through the Web UI to validate the timeout feature:

```groovy
println(1)
Thread.sleep(1000)
println(2)
Thread.sleep(1000)
println(3)
Thread.sleep(1000)
println(4)
Thread.sleep(1000)
println(5)
Thread.sleep(1000)
println(6)
Thread.sleep(1000)
println(7)
Thread.sleep(1000)
println(8)
Thread.sleep(1000)
println(9)
Thread.sleep(1000)
println(10)
Thread.sleep(1000)
println(11)
Thread.sleep(1000)
```

When submitted, the program above should NOT execute fully. Only some of the first digits should print but the default timeout of 10 seconds should cause the task to be killed early (note: some time is lost loading the container, the JDK, the Groovy classes and the script), so less than 10 actual seconds are available to run the program.

---

**Creating more tasks than the size of the task pool**

Create 20 tasks that take several seconds to complete (useful to test the size of the task pool). In the Bash script below, each task prints its index, waits 5 seconds, then prints "ok"


```
for i in $(seq 1 20); do curl -X POST localhost:8080/api/tasks -H 'Content-type:application/json' -H "Authorization: Bearer $APITOKEN" -d "{\"name\": \"task $i\",\"lang\":\"groovy\",\"code\": \"println($i)\nsleep(5000)\nprintln('ok')\"}" ; echo; done
```

---

**A simple program that throws an Exception**

```
$ curl --no-progress-meter -X POST localhost:8080/api/tasks -H 'Content-type:application/json' -H "Authorization: Bearer $APITOKEN" -d '{"name": "example", "lang": "groovy", "code": "println(1/0)"}'
```


---

**Error example**

```
curl -X POST localhost:8080/api/tasks -H 'Content-type:application/json' -H "Authorization: Bearer $APITOKEN" -d '{"name": "invalid task", "lang": "bad-image-name"}'
```
---

**A task that submits another task by POSTing to the same service**

This is interesting because in theory it opens the doors to craft a *self-replicating task*:

```groovy
baseUrl = new URL('http://localhost:8080/api/tasks')
requestBody = '{"name":"children task","lang":"groovy","code":"println(1)"}'
connection = baseUrl.openConnection()
connection.setRequestProperty('Content-Type', 'application/json')
connection.with {
  doOutput = true
  requestMethod = 'POST'
  outputStream.withWriter { writer ->
    writer << requestBody
  }
  println content.text.length()
}
```

Submitting the code above will create a task, and executing the task will submit a POST request to the `/api/tasks` endpoint, creating *a new task* with the name `children task` and a small program that just prints `1`.

Crafting a code payload that in turn is able to create another similar, self-replicating payload is left as an exercise to the reader.

---

**Perl example**

An interesting thing about the `groovy` container is that it also has Perl installed. By default, the `perl` interpreter is also whitelisted in `application.properties` so you can submit Perl programs too.

In order to submit Perl code from the Web UI, you'll need to update the value of a hidden parameter. From the Web UI, click on the *Create Task* button to show the task creation form.

Open the developer tools in your browser (eg. press `F12`) and enter the following in the JavaScript console:

```
document.forms[0].scriptLang.value="perl"
```

Now, you can add Perl code in the text area. Use this copy of a classic, esoteric program to test it: https://gist.github.com/dfuenzalida/4cf84e83e5db002199c24dcdda121904


## Useful link references

* https://docs.docker.com/engine/reference/run/#runtime-constraints-on-resources
* https://stackoverflow.com/questions/48299352/how-to-limit-docker-run-execution-time
* https://stackoverflow.com/a/48299490/483566
* https://www.gnu.org/software/coreutils/manual/html_node/timeout-invocation.html
* https://subscription.packtpub.com/book/application_development/9781849519366/8/ch08lvl1sec89/executing-an-http-post-request