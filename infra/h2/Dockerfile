# H2 running in server mode, so the database is its own container instead of being
# embedded inside each service. One server hosts a separate database per service.
FROM eclipse-temurin:17-jre-noble

# Keep this in step with the H2 client version Spring Boot manages for the services
# (Spring Boot 4.1.x -> H2 2.4.240). The TCP protocol expects matching versions.
ARG H2_VERSION=2.4.240
ARG H2_SHA256=29b70e427cc1c40cdc376283adbb0cc62853073797bb5fe5761f81fe73d57ce0

RUN useradd --system --uid 10001 --home-dir /opt/h2 h2 \
 && mkdir -p /opt/h2 /opt/h2-data \
 && curl -fsSL -o /opt/h2/h2.jar \
      "https://repo1.maven.org/maven2/com/h2database/h2/${H2_VERSION}/h2-${H2_VERSION}.jar" \
 && echo "${H2_SHA256}  /opt/h2/h2.jar" | sha256sum -c - \
 && chown -R h2:h2 /opt/h2 /opt/h2-data

COPY --chown=h2:h2 entrypoint.sh /opt/h2/entrypoint.sh
# Guard against CRLF line endings if the repo was checked out on Windows without .gitattributes.
RUN sed -i 's/\r$//' /opt/h2/entrypoint.sh && chmod 0755 /opt/h2/entrypoint.sh

USER h2
VOLUME ["/opt/h2-data"]
EXPOSE 9090 8090

ENTRYPOINT ["/opt/h2/entrypoint.sh"]
