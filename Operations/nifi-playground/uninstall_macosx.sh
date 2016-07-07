echo "Uninstalling all Docker containers and nifi-playground VirtualBox VM"

while true; do
    read -p "Do you really wish to delete all previously built Docker containers for the nifi-playground VM?" yn
    case $yn in
        [Yy]* ) docker-machine kill nifi-playground; docker-machine rm nifi-playground; break;;
        [Nn]* ) exit;;
        * ) echo "Please answer yes or no.";;
    esac
done