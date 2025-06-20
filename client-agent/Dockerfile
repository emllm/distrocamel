FROM groovy:4.0.0-jdk11

WORKDIR /app
COPY client.groovy .

# Create deployment directory
RUN mkdir -p /app/deployments

CMD ["groovy", "client.groovy"]
