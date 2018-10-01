
FROM ubuntu:16.04
MAINTAINER James Alastair McLaughlin <j.a.mclaughlin@ncl.ac.uk>

RUN apt-get update && apt-get install -y default-jdk maven

RUN mkdir /opt/svg-to-pptx
COPY pom.xml /opt/svg-to-pptx/
COPY src /opt/svg-to-pptx/src
RUN ls /opt/svg-to-pptx

RUN cd /opt/svg-to-pptx && mvn package
RUN find /opt/svg-to-pptx

EXPOSE 8080

COPY startup.sh /
RUN chmod +x /startup.sh

ENTRYPOINT ["/startup.sh"]



