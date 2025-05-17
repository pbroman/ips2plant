FROM alpine:latest
RUN apk add --no-cache libxslt && apk add --no-cache bash && apk add --no-cache sed
WORKDIR /wrk
ENTRYPOINT ["./ips2plant.sh"]