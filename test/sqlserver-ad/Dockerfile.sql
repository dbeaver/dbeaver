FROM mcr.microsoft.com/mssql/server:2022-latest
USER root
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends krb5-user dnsutils ldap-utils \
    && rm -rf /var/lib/apt/lists/*
USER mssql
