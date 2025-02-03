FROM bitnami/spark:latest

USER root

# Install core utilities (optional, but recommended)
RUN apt-get update && apt-get install -y --no-install-recommends \
    adduser && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Create a user with UID 1001
RUN adduser --disabled-password --gecos "" --uid 1001 spark

# Copy JARs and set permissions
COPY target/payment-spark-1.0-SNAPSHOT.jar /opt/spark/jars/
COPY target/spark-sql-kafka-0-10_2.12-3.5.1.jar /opt/spark/jars/
RUN chown -R 1001:1001 /opt/spark/jars

USER 1001
