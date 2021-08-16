#!/bin/bash

service nifi stop

# Clean up from the previous run
rm -rf /opt/nifi/latest/logs
rm -rf /opt/nifi/latest/content_repository/
rm -rf /opt/nifi/latest/database_repository/
rm -rf /opt/nifi/latest/flowfile_repository/
rm -rf /opt/nifi/latest/provenance_repository/
rm -rf /opt/nifi/latest/run
rm -rf /opt/nifi/latest/work
rm -rf /opt/nifi/latest/state
rm /opt/nifi/latest/lib/nifi-nvidia-*.nar

# Install the new artifacts
mv ./nifi-nvidia-service-nar/target/nifi-nvidia-service-nar-*.nar /opt/nifi/latest/lib/.
mv ./nifi-nvidia-nar/target/nifi-nvidia-nar-*.nar /opt/nifi/latest/lib/.

service nifi start
