### Local setup

We create a 3 node mongodb `ReplicaSet` using docker containers. 

#### Add the following the `/etc/hosts`

```bash
# mongodb cluster configuration
127.0.0.1 mongo-node1
127.0.0.1 mongo-node2
127.0.0.1 mongo-node3
```

#### Docker setup

##### Create a network

Since when creating the `ReplicaSet` we want to use container names to resolve the IP address we need to create a `user-defined` network, with the default `bridge` network it will not work. 

```bash
docker network create my-mongo-cluster
```

##### Start up the containers

When creating the containers we reference the user-defined network with the `--net` option.  

```bash
docker container run -d --name mongo-node1 \
    -v $(pwd)/mongodb-cluster/data/db/node1:/data/db \
    -v $(pwd)/mongodb-cluster/data/configdb/node1:/data/configdb \
    -v $(pwd)/mongodb-cluster/config/node1:/etc/mongo \
    --net my-mongo-cluster \
    -p 27017:27017 \
    mongo:4.0.6 --config /etc/mongo/mongod.config
     
docker container run -d --name mongo-node2 \
    -v $(pwd)/mongodb-cluster/data/db/node2:/data/db \
    -v $(pwd)/mongodb-cluster/data/configdb/node2:/data/configdb \
    -v $(pwd)/mongodb-cluster/config/node2:/etc/mongo \
    --net my-mongo-cluster \
    -p 27018:27018 \
    mongo:4.0.6 --config /etc/mongo/mongod.config
 
docker container run -d --name mongo-node3 \
    -v $(pwd)/mongodb-cluster/data/db/node3:/data/db \
    -v $(pwd)/mongodb-cluster/data/configdb/node3:/data/configdb \
    -v $(pwd)/mongodb-cluster/config/node3:/etc/mongo \
    --net my-mongo-cluster \
    -p 27019:27019 \
    mongo:4.0.6 --config /etc/mongo/mongod.config
    
    ```

##### Verify that they are up and running

```bash
docker ps -a
```

```bash
CONTAINER ID        IMAGE               COMMAND                  CREATED             STATUS              PORTS                                 NAMES
dae526ee43c8        mongo:4.0.6         "docker-entrypoint.s…"   About an hour ago   Up 34 minutes       27017/tcp, 0.0.0.0:27019->27019/tcp   mongo-node3
af4623782ce5        mongo:4.0.6         "docker-entrypoint.s…"   About an hour ago   Up 24 minutes       27017/tcp, 0.0.0.0:27018->27018/tcp   mongo-node2
caac34cf1e57        mongo:4.0.6         "docker-entrypoint.s…"   About an hour ago   Up 7 minutes        0.0.0.0:27017->27017/tcp              mongo-node1
```

##### Initialize the `ReplicatSet`
 

```bash
docker exec -it mongo-node1 bash
mongo
```

```bash
config = {
      "_id" : "demo",
      "members" : [
          {
              "_id" : 0,
              "host" : "mongo-node1:27017"
          },
          {
              "_id" : 1,
              "host" : "mongo-node2:27018"
          },
          {
              "_id" : 2,
              "host" : "mongo-node3:27019"
          }
      ]
  }

rs.initiate(config)
```


##### Check the status

```bash
demo:PRIMARY> rs.status()
```

##### MongoDB GUI (Robo 3T)

```bash
brew cask install robo-3t
```

`ReplicatSet` connection configuration

![DemoClusterMongoGUI](../altfatterz.github.io/images/2019-03-26/DemoClusterMongoGUI.png)

##### Mongo client connection

```bash
brew install mongo
``` 

```bash
mongo --host demo/mongo-node1:27017,mongo-node2:27018,mongo-node3:27019 test
```

this uses the connection url:

```bash
mongodb://mongo-node1:27017,mongo-node2:27018,mongo-node3:27019/test?replicaSet=demo
``` 

##### Spring Data MongoDB connection

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017,localhost:27018,localhost:27019/test?replicaSet=demo
```

##### Useful MongoDB commands

```bash
show dbs
show collections
```

#### Useful docker commands

It is important not to allow a running container to consume too much of the host machine’s memory.
By default, each container’s access to the host machine’s CPU cycles is unlimited.

##### Limit a container’s access to memory

```bash
docker run -it --memory=1g ubuntu /bin/bash
```

```bash
docker stats

CONTAINER ID        NAME                CPU %               MEM USAGE / LIMIT     MEM %               NET I/O             BLOCK I/O           PIDS
cf04d3bd32b1        trusting_wu         0.00%               744KiB / 1GiB         0.07%               1.11kB / 0B         0B / 0B             1
```

##### CPU

Specify how much of the available CPU resources a container can use.
For instance, if the host machine has 4 CPUs (check with `nproc` command) and you set --cpus=1.5, the container is guaranteed at most one and a half of the CPUs.

```bash
docker run -it --cpus=1.5 ubuntu /bin/bash
```

#### Authentication

##### Create `admin` user

-- do this after creating the `ReplicaSet` without turning on authentication

```bash
use admin

admin = {
    user: "admin",
    pwd: "s3cr3t",
    roles: [ { role: "root", db:"admin" } ]
}

db.createUser(admin)

show users   // you need to in admin database
show roles

db.dropUser(<username>)

``` 

#### Turn on authentication

Add the following into your `mongod.config` configuration files for all 3 nodes

```bash
security:
  authorization: enabled
  keyFile: /etc/mongo/mongodb.key
```

The `keyFile` is needed for authentication between servers in the `ReplicaSet`, not for logging in. It can be created:

```bash
openssl rand -base64 1024 > mongodb.key
chmod 600 mongodb.key
```

#### Restart the cluster

```bash
docker container restart mongo-node1 mongo-node2 mongo-node3
```

#### Test login

```bash
mongo mongodb://admin:s3cr3t@localhost:27017/admin

show users

demo:PRIMARY> show users
{
	"_id" : "admin.admin",
	"user" : "admin",
	"db" : "admin",
	"roles" : [
		{
			"role" : "userAdminAnyDatabase",
			"db" : "admin"
		}
	],
	"mechanisms" : [
		"SCRAM-SHA-1",
		"SCRAM-SHA-256"
	]
}
```

Note that you can still connect to the cluster, with `mongo` only to the `admin` database you need authentication.

```bash
mongo 
use admin
show users

Error: command usersInfo requires authentication 

db.auth("admin","s3cr3t")
show users 

demo:PRIMARY> show users
{
	"_id" : "admin.admin",
	"user" : "admin",
	"db" : "admin",
	"roles" : [
		{
			"role" : "userAdminAnyDatabase",
			"db" : "admin"
		}
	],
	"mechanisms" : [
		"SCRAM-SHA-1",
		"SCRAM-SHA-256"
	]
}
```


##### Create additional users

```bash
use test
user =   {
           "user": "john",
           "pwd": "johnpass",
           "roles": [ { "role": "readWrite", db: "test" } ]
         }
         
db.createUser(user)         
``` 

##### Test connection

```bash
mongo mongodb://john:johnpass@localhost:27017/test

db.customer.find()   // should work
```

Note that with the `john` user you don't have access to `show users` and `show roles`, since we just gave `readWrite` role to the `john` user


##### Cleanup locally

```bash
docker container stop mongo-node1 mongo-node2 mongo-node3
docker container rm mongo-node1 mongo-node2 mongo-node3
```

##### Running on GCP

Create a Google VM with checkbox `Deploy a container image to this VM instance` and provide a docker images like `mongo`


Useful when for example `ping` not found

```bash
docker run --rm busybox ping SERVER_NAME -c 2
```

##### With one host

```bash
docker run --name mongo-node1 -p 27017:27017 -d mongo:4.0.6 --replSet "demo"
docker run --name mongo-node2 -p 27018:27017 -d mongo:4.0.6 --replSet "demo"
docker run --name mongo-node3 -p 27019:27017 -d mongo:4.0.6 --replSet "demo"
```


```bash
config = {
      "_id" : "demo",
      "members" : [
          {
              "_id" : 0,
              "host" : "mongodb-docker2:27017"
          },
          {
              "_id" : 1,
              "host" : "mongodb-docker2:27018"
          },
          {
              "_id" : 2,
              "host" : "mongodb-docker2:27019"
          }
      ]
  }
rs.initiate(config)
```

##### With two hosts

Containers on `mongo-docker2`

```bash
docker run --name mongo-node1 -p 27017:27017 -d mongo:4.0.6 --replSet "demo"
docker run --name mongo-node2 -p 27018:27017 -d mongo:4.0.6 --replSet "demo"
```

Containers on `mongo-docker3`

```bash
docker run --name mongo-node3 -p 27017:27017 -d mongo:4.0.6 --replSet "demo"
```

```bash
config = {
      "_id" : "demo",
      "members" : [
          {
              "_id" : 0,
              "host" : "34.65.238.44:27017"
          },
          {
              "_id" : 1,
              "host" : "34.65.238.44:27018"
          },
          {
              "_id" : 2,
              "host" : "34.65.110.140:27017"
          }
      ]
  }
rs.initiate(config)
```


Remove volumes
```bash
docker volume ls | grep '^local' | awk '{ print "docker volume rm " $2}' | bash
```



##### Using single MongoDB instance backup and restore


1. Start the mongo instance

```bash
docker container run -d --name mongo-node \
    -v $(pwd)/mongodb/data/db:/data/db \
    -p 27017:27017 \
    mongo:4.0.6
```

2. Start the spring instance

3. Verify that documents are inserted every 200 milliseconds

4. Backup the `test` database in the `mongodb-backup` folder
```bash
mongodump -d test -o mongodb-backup
2019-03-27T13:06:59.943+0100	writing test.customer to
2019-03-27T13:06:59.953+0100	done dumping test.customer (954 documents)


With `mongodump` is recomended to connect to the secondary in a replica set 

```

5.  Check the database count

```bash
db.customer.count()
1014
```

6. Restore the database

`restored-test` is the name of the new database and `mongodb-backup/test` is the directory where we dumped the database `test` 

```bash
mongorestore -d restored-test mongodb-backup/test
```





Resources:

- https://hub.docker.com/_/mongo
- https://stackoverflow.com/questions/31839777/how-to-configure-spring-data-mongodb-to-use-a-replica-set-via-properties
- https://docs.spring.io/spring-boot/docs/2.0.8.RELEASE/reference/htmlsingle/#boot-features-connecting-to-mongodb
- http://www.tugberkugurlu.com/archive/setting-up-a-mongodb-replica-set-with-docker-and-connecting-to-it-with-a--net-core-app
- https://medium.com/mongoaudit/how-to-enable-authentication-on-mongodb-b9e8a924efac
- https://stackoverflow.com/questions/38524150/mongodb-replica-set-with-simple-password-authentication
- https://serverfault.com/questions/424465/how-to-reset-mongodb-replica-set-settings

- https://stackoverflow.com/questions/14825557/does-mongodump-lock-the-database
- https://docs.mongodb.com/manual/core/backups/#backup-with-mongodump