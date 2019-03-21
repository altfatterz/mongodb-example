```bash
mkdir ~/mongodb-cluster/node1
mkdir ~/mongodb-cluster/node2
mkdir ~/mongodb-cluster/node3
```

```bash
docker network create my-mongo-cluster
```

```bash
docker run --name mongo-node1 -d --net my-mongo-cluster -v ~/mongodb-cluster/node1:/data/db -p 27017:27017 mongo:4.0.6 --replSet "helsana"
docker run --name mongo-node2 -d --net my-mongo-cluster -v ~/mongodb-cluster/node2:/data/db -p 27018:27017 mongo:4.0.6 --replSet "helsana"
docker run --name mongo-node3 -d --net my-mongo-cluster -v ~/mongodb-cluster/node3:/data/db -p 27019:27017 mongo:4.0.6 --replSet "helsana"
```

```bash
docker ps -a
```

```bash
CONTAINER ID        IMAGE               COMMAND                  CREATED             STATUS              PORTS                      NAMES
6cccc072c175        mongo:4.0.6         "docker-entrypoint.s…"   7 minutes ago       Up 7 minutes        0.0.0.0:27019->27017/tcp   mongo-node3
640cf2823a03        mongo:4.0.6         "docker-entrypoint.s…"   7 minutes ago       Up 1 second         0.0.0.0:27018->27017/tcp   mongo-node2
b3d231c3c1b8        mongo:4.0.6         "docker-entrypoint.s…"   7 minutes ago       Up 7 minutes        0.0.0.0:27017->27017/tcp   mongo-node1
```

