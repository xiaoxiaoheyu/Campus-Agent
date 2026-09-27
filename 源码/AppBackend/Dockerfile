ARG DOCKER_HUB_MIRROR=docker.1ms.run/library
FROM ${DOCKER_HUB_MIRROR}/maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
ENV PLAYWRIGHT_DOWNLOAD_HOST=https://cdn.npmmirror.com/binaries/playwright

COPY pom.xml ./
RUN mkdir -p /root/.m2 && printf '%s' '<settings><mirrors><mirror><id>aliyun</id><name>aliyun</name><url>https://maven.aliyun.com/repository/public</url><mirrorOf>central</mirrorOf></mirror></mirrors></settings>' > /root/.m2/settings.xml
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package \
    && artifact="$(find /build/target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' -print -quit)" \
    && test -n "$artifact" \
    && test -s "$artifact" \
    && cp "$artifact" /build/app.jar \
    && jar tf /build/app.jar >/dev/null
# npmmirror 只镜像了完整版 Chromium，没有 chromium-headless-shell；
# 因此直接从镜像站点取完整包并手动放置，运行时配合
# PLAYWRIGHT_CHROMIUM_USE_HEADLESS_NEW=1 使用完整浏览器跑无头模式。
RUN curl -fsSL --retry 3 -o /tmp/chromium.zip \
        https://cdn.npmmirror.com/binaries/playwright/builds/chromium/1148/chromium-linux.zip \
    && mkdir -p /ms-playwright/chromium-1148 \
    && cd /ms-playwright/chromium-1148 \
    && jar xf /tmp/chromium.zip \
    && touch INSTALLATION_COMPLETE \
    && rm -f /tmp/chromium.zip

ARG DOCKER_HUB_MIRROR=docker.1ms.run/library
FROM ${DOCKER_HUB_MIRROR}/eclipse-temurin:21-jre-jammy
WORKDIR /app

ENV TZ=Asia/Shanghai \
    PLAYWRIGHT_BROWSERS_PATH=/ms-playwright \
    PLAYWRIGHT_CHROMIUM_USE_HEADLESS_NEW=1

RUN sed -i \
        -e 's|http://archive.ubuntu.com/ubuntu|https://mirrors.aliyun.com/ubuntu|g' \
        -e 's|http://security.ubuntu.com/ubuntu|https://mirrors.aliyun.com/ubuntu|g' \
        /etc/apt/sources.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        ffmpeg \
        fonts-noto-cjk \
        fonts-wqy-zenhei \
        libasound2 \
        libatk-bridge2.0-0 \
        libatk1.0-0 \
        libatspi2.0-0 \
        libcairo2 \
        libcups2 \
        libdbus-1-3 \
        libdrm2 \
        libgbm1 \
        libglib2.0-0 \
        libnspr4 \
        libnss3 \
        libpango-1.0-0 \
        libwayland-client0 \
        libx11-6 \
        libxcb1 \
        libxcomposite1 \
        libxdamage1 \
        libxext6 \
        libxfixes3 \
        libxkbcommon0 \
        libxrandr2 \
        libreoffice-writer \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /ms-playwright /ms-playwright
COPY --from=builder /build/app.jar /app/app.jar
RUN test -s /app/app.jar

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=12 \
  CMD curl --fail --silent http://127.0.0.1:8080/actuator/health >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
