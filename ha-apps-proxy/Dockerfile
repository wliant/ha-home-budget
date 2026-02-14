FROM nginx:alpine

RUN apk add --no-cache curl jq

COPY nginx.conf.template /nginx.conf.template
COPY run.sh /run.sh
RUN chmod +x /run.sh
COPY error.html /usr/share/nginx/html/error.html

CMD ["/run.sh"]
