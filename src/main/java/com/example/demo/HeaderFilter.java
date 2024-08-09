package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(-1)
public class HeaderFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(HeaderFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var request = exchange.getRequest();
        var response = exchange.getResponse();

        String acceptHeader = request.getHeaders().getFirst("Accept");
        String contentType = request.getHeaders().getFirst("Content-Type");
        boolean isPostOrPut = HttpMethod.POST.equals(request.getMethod()) || HttpMethod.PUT.equals(request.getMethod());

        // Logowanie nieprawidłowych nagłówków i zwrócenie odpowiedzi 406
        if (!MediaType.APPLICATION_JSON_VALUE.equals(acceptHeader)) {
            logger.warn("Invalid Accept header: {}", acceptHeader);
            response.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            String errorMessage = String.format(
                    "{\"status\": %d, \"message\": \"Accept header must be application/json. Received: %s\"}",
                    HttpStatus.NOT_ACCEPTABLE.value(), acceptHeader
            );

            DataBuffer buffer = response.bufferFactory().wrap(errorMessage.getBytes());
            return response.writeWith(Mono.just(buffer));
        }

        if (isPostOrPut && !MediaType.APPLICATION_JSON_VALUE.equals(contentType)) {
            logger.warn("Invalid Content-Type header for {} request: {}", request.getMethod(), contentType);
            response.setStatusCode(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            return response.setComplete();
        }

        return chain.filter(exchange);
    }
}
