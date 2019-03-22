### Host configurations

#### Create the data directories

```bash
mkdir ~/mongodb-cluster/data/node1
mkdir ~/mongodb-cluster/data/node2
mkdir ~/mongodb-cluster/data/node3
```

#### Create the config directories

```bash
mkdir ~/mongodb-cluster/config/node1
mkdir ~/mongodb-cluster/config/node2
mkdir ~/mongodb-cluster/config/node3
```

Add a `mongod.config` into each of the `node1`, `node2` and `node3` configuration folders with the following content

```yaml
replication:
  oplogSizeMB: 1024
  replSetName: helsana
net:
  port: 27017
```

```yaml
replication:
  oplogSizeMB: 1024
  replSetName: helsana
net:
  port: 27018
```

```yaml
replication:
  oplogSizeMB: 1024
  replSetName: helsana
net:
  port: 27019
```

#### Add the following the `/etc/hosts`


```bash
# mongodb cluster configuration
127.0.0.1 mongo-node1
127.0.0.1 mongo-node2
127.0.0.1 mongo-node3
```

#### Docker setup

##### Create a network

```bash
docker network create my-mongo-cluster
```

##### Start up the containers

```bash
docker run --name mongo-node1 -d --net my-mongo-cluster -v ~/mongodb-cluster/data/node1:/data/db -v ~/mongodb-cluster/config/node1:/etc/mongo -p 27017:27017 mongo:4.0.6 --config /etc/mongo/mongod.config 
docker run --name mongo-node2 -d --net my-mongo-cluster -v ~/mongodb-cluster/data/node2:/data/db -v ~/mongodb-cluster/config/node2:/etc/mongo -p 27018:27018 mongo:4.0.6 --config /etc/mongo/mongod.config 
docker run --name mongo-node3 -d --net my-mongo-cluster -v ~/mongodb-cluster/data/node3:/data/db -v ~/mongodb-cluster/config/node3:/etc/mongo -p 27019:27019 mongo:4.0.6 --config /etc/mongo/mongod.config 
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
      "_id" : "helsana",
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
helsana:PRIMARY> rs.status()
```

##### MongoDB GUI (Robo 3T)

```bash
brew cask install robo-3t
```

ReplicatSet connection configuration

![robo3T](robo3T.png)

##### Mongo client connection

```bash
brew install mongo
``` 

```bash
mongo --host helsana/mongo-node1:27017,mongo-node2:27018,mongo-node3:27019 test
```

this uses the connection url:

```bash
mongodb://mongo-node1:27017,mongo-node2:27018,mongo-node3:27019/test?replicaSet=helsana
``` 

##### Spring Data MongoDB connection

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017,localhost:27018,localhost:27019/test?replicaSet=helsana
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
    roles: [ { role: "userAdminAnyDatabase", db:"admin" } ]
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
openssl rand -base64 741 > mongodb.key
chmod 600 mongodb.key
```

#### Restart the cluster

```bash
docker stop mongo-node1
docker stop mongo-node2
docker stop mongo-node3

docker start mongo-node1
docker start mongo-node2
docker start mongo-node3
```

#### Test login

```bash
mongo mongodb://admin:s3cr3t@localhost:27017/admin

show users

helsana:PRIMARY> show users
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

helsana:PRIMARY> show users
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


Resources:

- https://hub.docker.com/_/mongo
- https://stackoverflow.com/questions/31839777/how-to-configure-spring-data-mongodb-to-use-a-replica-set-via-properties
- https://docs.spring.io/spring-boot/docs/2.0.8.RELEASE/reference/htmlsingle/#boot-features-connecting-to-mongodb
- http://www.tugberkugurlu.com/archive/setting-up-a-mongodb-replica-set-with-docker-and-connecting-to-it-with-a--net-core-app
- https://medium.com/mongoaudit/how-to-enable-authentication-on-mongodb-b9e8a924efac
- https://stackoverflow.com/questions/38524150/mongodb-replica-set-with-simple-password-authentication