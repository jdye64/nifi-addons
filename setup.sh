echo "Setting up Docker infrastructure for Service Discovery and DNS capabilities"

MANAGER_IP=$(docker-machine ip manager)
AGENT1_IP=$(docker-machine ip agent1)
AGENT2_IP=$(docker-machine ip agent2)

if [ ! $MANAGER_IP ]; then
	echo "No 'manager' virtualbox VM available. Creating one now"
	docker-machine create -d virtualbox manager
	MANAGER_IP=$(docker-machine ip manager)
fi

if [ ! $AGENT1_IP ]; then
	echo "No 'agent1' virtualbox VM available. Creating one now"
	docker-machine create -d virtualbox agent1
	AGENT1_IP=$(docker-machine ip agent1)
fi

if [ ! $AGENT2_IP ]; then
	echo "No 'agent2' virtualbox VM available. Creating one now"
	docker-machine create -d virtualbox agent2
	AGENT2_IP=$(docker-machine ip agent2)
fi

echo "'manager ip -> $MANAGER_IP"
echo "'agent1' ip -> $AGENT1_IP"
echo "'agent2' ip -> $AGENT2_IP"

echo "Installing on 'manager' VM Machine"
eval $(docker-machine env manager)
docker rm -f consul
docker rm -f registrator
docker rm -f docker-http
docker rm -f swarm
docker rm -f testweb
docker run -d -p 2375:2375 --volume=/var/run/docker.sock:/var/run/docker.sock --name=docker-http sequenceiq/socat
docker run --name consul -e SERVICE_IGNORE=true -d -p 8300:8300 -p 8301:8301/tcp -p 8301:8301/udp -p 8302:8302/tcp -p 8302:8302/udp -p 8400:8400 -p 8500:8500 -p 0.0.0.0:53:8600/udp gliderlabs/consul-server:0.5 --advertise $MANAGER_IP -recursor 8.8.8.8 -bootstrap
docker run --name registrator -d -v /var/run/docker.sock:/tmp/docker.sock --privileged --link consul:local gliderlabs/registrator:v5 consul://local:8500
docker run --name swarm -d -p $MANAGER_IP:3376:3376 swarm manage -H 0.0.0.0:3376 nodes://$MANAGER_IP:2375,$AGENT1_IP:2375$AGENT2_IP:2375
docker run --name testweb -d -p 80:80 -e SERVICE_NAME=testweb nginx

echo "Installing on Second VM Machine"
eval $(docker-machine env agent1)
docker rm -f consul
docker rm -f registrator
docker rm -f docker-http
docker rm -f testweb
docker run -d -p 2375:2375 --volume=/var/run/docker.sock:/var/run/docker.sock --name=docker-http sequenceiq/socat
docker run --name consul -e SERVICE_IGNORE=true -d -p 8300:8300 -p 8301:8301/tcp -p 8301:8301/udp -p 8302:8302/tcp -p 8302:8302/udp -p 8400:8400 -p 8500:8500 -p 0.0.0.0:53:8600/udp gliderlabs/consul-server:0.5 --advertise $AGENT1_IP -recursor 8.8.8.8 -join $MANAGER_IP
docker run --name registrator -d -v /var/run/docker.sock:/tmp/docker.sock --privileged --link consul:local gliderlabs/registrator:v5 consul://local:8500
docker run --name testweb -d -p 80:80 -e SERVICE_NAME=testweb nginx

echo "Installting on Third VM Machine"
eval $(docker-machine env agent2)
docker rm -f consul
docker rm -f registrator
docker rm -f docker-http
docker rm -f testweb
docker run -d -p 2375:2375 --volume=/var/run/docker.sock:/var/run/docker.sock --name=docker-http sequenceiq/socat
docker run --name consul -e SERVICE_IGNORE=true -d -p 8300:8300 -p 8301:8301/tcp -p 8301:8301/udp -p 8302:8302/tcp -p 8302:8302/udp -p 8400:8400 -p 8500:8500 -p 0.0.0.0:53:8600/udp gliderlabs/consul-server:0.5 --advertise $AGENT2_IP -recursor 8.8.8.8 -join $MANAGER_IP
docker run --name registrator -d -v /var/run/docker.sock:/tmp/docker.sock --privileged --link consul:local gliderlabs/registrator:v5 consul://local:8500
docker run --name testweb -d -p 80:80 -e SERVICE_NAME=testweb nginx

# Open the Consul UI to view the nodes and containers running
open http://$MANAGER_IP:8500

# Set the Swarm DOCKER_HOST
export DOCKER_HOST=tcp://$MANAGER_IP:3376

# Starts the desired Docker containers
docker run -d -p 8080:8080 -e SERVICE_NAME=tesseract jdye64/nifi-tesseract:1.5