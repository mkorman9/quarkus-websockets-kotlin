FROM eclipse-temurin:21-jre

COPY --chown=nobody:nogroup target/quarkus-app/lib/ /runtime/lib/
COPY --chown=nobody:nogroup target/quarkus-app/*.jar /runtime/
COPY --chown=nobody:nogroup target/quarkus-app/app/ /runtime/app/
COPY --chown=nobody:nogroup target/quarkus-app/quarkus/ /runtime/quarkus/

USER nobody
WORKDIR /runtime

EXPOSE 8080

CMD exec java ${JAVA_OPTS} -jar quarkus-run.jar
