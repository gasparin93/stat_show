**Sparky Weekend at the PubSub**

**By Edgar Sias**

**High Level Design**

I thought I’d spend Friday evening after work fiddling around with Spark and Kafka. I had thrown my back out carrying two heavy kids a few days earlier, so I had a good excuse to sit down and crunch bytecode. I did not have any Spark experience at the time and I only had user-side experience of Kafka, since the SRE teams do any setups and configurations of Kafka. I figured a simple use-case would be ideal: a web application that has a way to generate batch payments. The front-end would include:

- Two sections:
    - Section 1: The lower half of the page, where there would be a simple form:
        - One text box for Strings representing a user.
        - One text box for floats representing a payment amount.
        - One text box for positive integers representing the volume of payments for that user for that amount.
        - A button to submit to the server.
            - This would send a json representation of the payments to a REST endpoint which would then forward these to a Kafka topic _payments_
            - Spark would then read from this topic, apply aggregation analytics to sum the payment amounts and counts per user. It would then take these aggregates and push to Kafka topic _payment-stats._
            - The Java backend that also hosts the RESTful endpoints would be listening to the _payment-stats_ topic and updating an in-memory repository (a concurrent hash map) to be made available for stat requests as the updates came in.
    - Section 2: The upper half of the page, a simple list of aggregated payments bucketed by user, ordered by sum payment amounts.
        - This information would come from a REST endpoint that would query the in-memory repository of the backend Java server. This information would be coming in through Spark which would push it to the _payment-stats_ topic that the server would listen to.
            - There would be a “refresh” button to call this endpoint ad-hoc, but it would automatically be called a second after submitting payments automatically.
            - The front-end would replace the dom object (a div or table) with the new information every time it would hit the endpoint.
- This would all be a simple page leveraging jQuery and Ajax.

In essence:

1. User submits simple payment information
2. This is forwarded to Kafka
3. Spark picks it up from Kafka
4. Spark aggregates stats
5. Spark writes these aggregates to Kafka
6. Server caches these aggregates
7. User can then query for the aggregates

**Considerations**

- My computers are all Windows, so setting all this up would be like trying to do wheelies on a Toyota Prius.
- I also don’t want to load up my personal computers with Kafka or Spark.
- I have three Raspberry Pis:
    - Raspberry Pi 4B 4GB: currently running an nginx reverse proxy and an apache2 landing page.
    - Raspberry Pi 4B 8GB: currently running a MyBB forum and a Wikimedia wiki instance.
    - Raspberry Pi 5 8GB: currently collecting dust, Kubernetes halfway installed.
- The Pi5 would be the best bet. However, given the limited resources, I did not want to install too many things that would slow it down in the long-run. As such, I figured the best way to move forward would be to finish installing Kubernetes (k3s).
- However, given that one of the other Pis was running pages I use regularly, and the other one was handling reverse proxy for them to be visible to the outside world through HTTP ports, then the Pi5 would have to be a single-node cluster.
- StackOverflow and publicly-available LLMs would be my helpers to get familiar with the new technologies.

**Assumptions**

- I could do this, best case scenario, on a Friday night, and worst-case scenario in pauses over the weekend.
- The Pi5 would be able to function as a single-node cluster to concurrently handle both Spark and Kafka, assuming very simple messages being processed.
- The Pi5’s ARM64 architecture would be compatible with some variant of the following technologies:
    - Kafka
    - Spark
    - Kubernetes
- One of my other Pis, ideally the 4B 8GB, would be able to handle a RESTful SpringBoot server to serve as the middle-man between Kafka and the User front-end.
- It would be fun and I wouldn’t pull out the last bit of remaining hair I have left.

**Conclusions**

- I was successfully able to install k3s Kubernetes to the Pi5.
- I was successfully able to deploy Kafka to the cluster.
- I was successfully able to deploy Spark to the cluster.
- I was successfully able to write and run the RESTful server to publish and subscribe to Kafka topics.
- I was successfully able to create, write to, and read from Kafka topics.
- I was successfully able to write and run (but not to completion) a Spark application using Java.
- The Pi5 simply did not have the resources to handle running Spark given the constraints.
- Given this, I did not bother writing the front-end.
- It was very fun and informative, and I only pulled a few hairs.
- I need to buy 3 more Pi5s to create a proper cluster and try again.
- DeepSeek is much better at understanding Dockerfile and k3s YAML manifest files than ChatGPT

**Journal**

- Initial step was to turn the Pi5 back on and finish installation of the Kubernetes variant k3s, which is created to work in resource-strapped environments like a Raspberry Pi. 
- This was simple enough, and I installed it successfully in about 30 minutes. Here’s the pods running as of this writing:  
```
esias@rasp5-1:~/configs/kafka_2.12-3.8.1/bin $ sudo k3s kubectl get pods -A
NAMESPACE NAME READY STATUS RESTARTS AGE
kafka my-cluster-entity-operator-fb4bcf5cf-kzvbf 2/2 Running 0 2d
kafka my-cluster-kafka-0 1/1 Running 0 4h6m
kafka my-cluster-kafka-1 1/1 Running 0 4h4m
kafka my-cluster-kafka-2 1/1 Running 0 4h5m
kafka my-cluster-zookeeper-0 1/1 Running 0 2d1h
kafka strimzi-cluster-operator-6bf566db79-8zdfx 1/1 Running 0 2d1h
kube-system coredns-ccb96694c-kgk5g 1/1 Running 0 2d6h
kube-system helm-install-traefik-crd-wjvmf 0/1 Completed 0 2d6h
kube-system helm-install-traefik-gd6jc 0/1 Completed 1 2d6h
kube-system local-path-provisioner-5cf85fd84d-rzw59 1/1 Running 0 2d6h
kube-system metrics-server-5985cbc9d7-btrj9 1/1 Running 0 2d6h
kube-system svclb-traefik-b88b7506-4mxgx 2/2 Running 0 2d6h
kube-system traefik-5d45fc8cc9-qgvnp 1/1 Running 0 2d6h
spark spark-master-6765d89b4c-p2j6b 1/1 Running 0 22h
spark spark-worker-84d6c4df79-ml677 1/1 Running 1 (22h ago) 22h
```
- Next, I had to install Kafka. Strimzi’s distribution seemed to be tailored towards running in k3s.
- That was simple enough. I created a cluster with a generic _my-cluster_ name. 
- I then created two topics: _payments_ to ingest raw payment information, and _payment-stats_ to ingest aggregated payment information.  
```
esias@rasp5-1:~/configs/kafka_2.12-3.8.1/bin $ sudo k3s kubectl get kafkatopics -n kafka
NAME          CLUSTER    PARTITIONS REPLICATION FACTOR READY
payment-stats my-cluster 1          1                  True
payments      my-cluster 1          1                  True
```

- This was done in about an hour while I was trying to understand and refine the manifest files, using my robot friends to help me break things down.
- Next step was writing the RESTful server to write to these. This was pretty simple, I created the Payment and Payment aggregate classes, leveraging Lombok to reduce boilerplate code. This took about an hour.
- I then spun up the server and tested out the endpoint that would publish to Kafka. To do this, I used Postman. 
- I then successfully verified that it actually did write to the topic, so we are looking good:  
```
esias@rasp5-1:~/configs/kafka_2.12-3.8.1/bin $ ./kafka-console-consumer.sh --bootstrap-server 192.168.5.193:32119 --topic payments --from-beginning
{"user":"edgar","paymentAmt":1.0}
{"user":"edgar","paymentAmt":1.0}
{"user":"edgar","paymentAmt":1.0}
{"user":"edgar","paymentAmt":1.0}
{"user":"edgar","paymentAmt":1.0}
{"user":"edgar","paymentAmt":1.0}
{"user":"edgar","paymentAmt":1.0}
^CProcessed a total of 7 messages
```

- Next, I wrote a simple Spark application in Java to subscribe to the _payments_ topic, aggregate, and write to the _payment-stats_ topic. It’s pretty difficult to find good examples of Spark with Java, whether it be Google’s quality going down the drain, or the seemingly small market share of Java in Spark. I ended up asking ChatGPT to write it, and then I skimmed through it to fix, refine and understand it.
- I then had to deploy Spark to k3s. I initially went with the operator route as helm was giving me issues, potentially PATH related, but I didn’t bother to debug.
- I was able to run the operator successfully, but when I spun up a task, it was issue after issue after issue. The ones I remember are:
    - Unable to find my Java Spark job Jar.
    - Unable to find or pull the Spark-Kafka Jar needed to access Kafka.
    - Ivy-related issues.
    - Hadoop-related issues.
    - Issues with permissions (this was due to me misunderstanding that the workingdir for Spark would be a staging directory where it deletes files).
    - Issues with user.
    - Issues that seemed to imply that the actual task settings were isolated from the manifest file settings due to the way operator would handle it.
- This whack-a-mole slugged on until Saturday night, when I figured I had had enough and forgot the operator route altogether. I nuked the namespace and anything related to it I could find, very happily. I smiled.
- I then asked my robot friend, the American one, what the best way to go about it would be given my constraints. It suggested the official spark image, which ended up being another rabbit hole of potential incompatibilities.
- I asked my robot friend, the Chinese one, the same thing, and it suggested Bitnami, which is a streamlined version made for containerization.
- I wrote a Dockerfile to account for any specific needs I had such as copying the Jar over properly.
- I created the master and worker manifest files and successfully deployed those to k3s.
- I then created and deployed the manifest file. I had many of the same issues as before, but the crucial difference was that there didn’t seem to be an intermediary layer between myself and the actual Spark job executor as with the orchestrator before.
- Some issues I encountered:
    - Ivy again. I decided to just nip it in the bud and make a fat Jar.
    - The initial setup was having Spark try to create the executors and not leverage the existing workers. I fixed this in the manifest file.
    - RBAC role-related issues. Since this was just me trying to leverage an enterprise grade tool created for big data analytics to crunch made up numbers that my toaster could aggregate, I fixed this by giving the running user cluster-admin privileges.
    - Hadoop issues. I fixed this by telling Spark that the file is local.
- After quite a bit of fiddling with both the Dockerfile and the job manifest file, I finally got Spark to initialize and start to process my Jar.
- I found that it would not actually do anything, it would only try to run it and immediately fail, and try again and again to spawn up new executors in an infinite loop.
- At first, I figured that I was overloading the Pi5 and scaled the workers down to 1, executors to 1, and scaled back the memory and CPU allocations for the executors to 1 core and 256MB.
- Spark complained about the memory being too low, saying it needed at least 450MB and a backrub.
- I then figured that it was actually not getting enough resources allocated to it, so I allocated more resources, 800MB RAM and 2 core.
- This actually did seem to work, as it would show it actually executing the Jar, but it would break before it did anything serious and continue its executor execution/revival purgatory.
- I then asked my robot friends to help me trim the fat from the Spark job. They suggested a few things, such as:
    - Adding a time-based trigger.
    - Adding a fixed batch size.
    - Using “update” instead of “complete” output mode.
- I tried to do this in stages. First, I added a 1 second trigger and 50 item bucketing. Then I did a 20-item bucketing. Then I did a 5 second trigger and an “update” output mode. Finally, I did a 10 second triggering.
- The job would get as far as reading from the Kafka topic and start the stream, but then crash and enter the same cycle.
- I additionally added proper slf4j logging, but that did not end up helping much, as it only seemed to confirm that it was a resource-constraint issue.
- I tried doubling the RAM to 2GB, but given the k3s metrics, this was a CPU issue. Although I gave it up to 3 cores to use, it did not ever get past the stage it was stuck on. It seemed that the Pi5’s limited resources were a hard cap to getting this working.
- I think the main takeaway was to either try to dig further to see if the issue was solveable with these constraints or to try this setup either with a beefier server or with a multi-node cluster of Pi5s.