FROM jenkins/jenkins:lts-jdk17

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl gnupg lsb-release \
    && install -m 0755 -d /etc/apt/keyrings \
    && curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc \
    && chmod a+r /etc/apt/keyrings/docker.asc \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo "$VERSION_CODENAME") stable" > /etc/apt/sources.list.d/docker.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends docker-ce-cli docker-compose-plugin \
    && git config --system --add safe.directory /workspace/taskmanager-service \
    && git config --system --add safe.directory /workspace/taskmanager-service/.git \
    && git config --system --add safe.directory /workspace/taskmanager-frontend \
    && git config --system --add safe.directory /workspace/taskmanager-frontend/.git \
    && git config --global --add safe.directory /workspace/taskmanager-service \
    && git config --global --add safe.directory /workspace/taskmanager-service/.git \
    && git config --global --add safe.directory /workspace/taskmanager-frontend \
    && git config --global --add safe.directory /workspace/taskmanager-frontend/.git \
    && rm -rf /var/lib/apt/lists/*

COPY init.groovy.d/ /usr/share/jenkins/ref/init.groovy.d/

USER jenkins
RUN git config --global --add safe.directory /workspace/taskmanager-service \
    && git config --global --add safe.directory /workspace/taskmanager-service/.git \
    && git config --global --add safe.directory /workspace/taskmanager-frontend \
    && git config --global --add safe.directory /workspace/taskmanager-frontend/.git \
    && jenkins-plugin-cli --plugins \
      git \
      workflow-aggregator \
      pipeline-stage-view \
      pipeline-graph-view
