
FROM jetty:jre8-alpine

ARG JAVAPS_VERSION=1.4.0
ENV JAVAPS_VERSION ${JAVAPS_VERSION}
ENV JAVAPS_ROOT ${JETTY_BASE}/webapps/ROOT
ENV JAVAPS_TMP ${JAVAPS_ROOT}/WEB-INF/tmp
ENV JAVAPS_CONFIG ${JAVAPS_ROOT}/WEB-INF/config
ENV JAVAPS_LIB ${JAVAPS_ROOT}/WEB-INF/lib

COPY webapp/target/webapp/ /var/lib/jetty/webapps/ROOT
COPY etc/docker-log4j2.xml /var/lib/jetty/webapps/ROOT/WEB-INF/config/log4j2.xml
COPY etc/docker-configuration.json /var/lib/jetty/webapps/ROOT/WEB-INF/config/configuration.json

# we need root access for the docker daemon
USER root
RUN set -ex \
 && apk add --no-cache jq \
 && wget -q -P /usr/local/bin https://raw.githubusercontent.com/52North/arctic-sea/master/etc/faroe-entrypoint.sh \
 && chmod +x /usr/local/bin/faroe-entrypoint.sh \
 && ln -sf ${JAVAPS_CONFIG}/log4j2.xml ${JAVAPS_ROOT}/WEB-INF/classes/log4j2.xml \
 && mkdir -p ${JAVAPS_TMP}\
 && chown -R jetty:jetty ${JAVAPS_ROOT}

VOLUME /var/lib/jetty/webapps/ROOT/WEB-INF/tmp
VOLUME /var/lib/jetty/webapps/ROOT/WEB-INF/config

#HEALTHCHECK --interval=5s --timeout=20s --retries=3 \
#  CMD wget 'http://localhost:8080/service?service=WPS&request=GetCapabilities' -q -O - > /dev/null 2>&1

ENV FAROE_CONFIGURATION ${JAVAPS_CONFIG}/configuration.json

LABEL maintainer="Christian Autermann <c.autermann@52north.org>" \
      org.opencontainers.image.title="52°North EOPAD WPS" \
      org.opencontainers.image.description="Next generation standardized web-based geo-processing" \
      org.opencontainers.image.licenses="Apache-2.0" \
      org.opencontainers.image.url="https://github.com/52North/eopad-javaps-backend" \
      org.opencontainers.image.vendor="52°North GmbH" \
      org.opencontainers.image.source="https://github.com/52north/eopad-javaps-backend.git" \
      org.opencontainers.image.documentation="https://github.com/52North/eopad-javaps-backend/blob/master/README.md" \
      org.opencontainers.image.version="${JAVAPS_VERSION}" \
      org.opencontainers.image.authors="Christian Autermann <c.autermann@52north.org>"

ARG GIT_COMMIT
LABEL org.opencontainers.image.revision="${GIT_COMMIT}"

ARG BUILD_DATE
LABEL org.opencontainers.image.created="${BUILD_DATE}"

ENV LISTENER_GMU_ENABLED="true" \
    LISTENER_GMU_USERNAME="" \
    LISTENER_GMU_PASSWORD="" \
    LISTENER_DEIMOS_ENABLED="true" \
    LISTENER_DEIMOS_USERNAME="" \
    LISTENER_DEIMOS_PASSWORD=""

CMD [ "java", "-jar", "/usr/local/jetty/start.jar" ]
ENTRYPOINT [ "/usr/local/bin/faroe-entrypoint.sh", "/docker-entrypoint.sh" ]
