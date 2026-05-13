package org.example.bff;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.setPath;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.TokenRelayFilterFunctions.tokenRelay;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@Configuration
public class BffConfig {

    @Bean
    SecurityFilterChain security(HttpSecurity http) {
        return http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.disable())
                // Enable OAuth2 login (for browser users)
                .oauth2Login(Customizer.withDefaults())
                // Enable OAuth2 client (needed for tokenRelay)
                .oauth2Client(Customizer.withDefaults())
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> route1() {
        // /api/test -> http://localhost:8081/api/test
        return route()
                .GET("/api/test", http())
                .before(uri("http://localhost:8081/"))
                .filter(tokenRelay())
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> route2() {
        // /api/test2 -> http://localhost:8082/api/test
        return route()
                .GET("/api/test2", http())
                .before(uri("http://localhost:8082/"))
                .before(setPath("/api/test"))         // Ändrar path till mottagaren
                .filter(tokenRelay())
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> route1WithSetPathAndSegment() {
        // /test -> http://localhost:8081/api/test
        return route()
                .GET("/{segment}", http())
                .before(uri("http://localhost:8081/"))
                .before(setPath("/api/{segment}"))
                .filter(tokenRelay())
                .build();
    }

//    /*
//     Ett vanligt scenario när man vill förenkla för sina microservices så att de slipper packa upp JWT-tokenet själva
//     är att istället för att använda tokenRelay(), som skickar vidare hela Authorization-headern, kan man använda
//     en kombination av Springs säkerhetskontext och filtret addRequestHeader.
//     */
//    @Bean
//    public RouterFunction<ServerResponse> routeWithUsername() {
//        // /api/test -> http://localhost:8081/api/test
//        return route()
//                .GET("/api/test", http())
//                //before är en specialiserad version av ett filter som är optimerat för att modifiera begäran innan den når handlaren.
//                //Begränsning: Den kan inte stoppa exekveringen och skicka tillbaka ett eget svar
//                //(t.ex. ett felmeddelande) på ett enkelt sätt. Den kan bara skicka vidare en modifierad version av begäran.
//                //Den tar emot en ServerRequest och returnerar en ServerRequest.
//                .before(uri("http://localhost:8081/"))
//                //filter är det mest kraftfulla verktyget. Det implementerar mönstret Chain of Responsibility.
//                //Ett filter "omsluter" hela anropet.
//                //Den tar emot en ServerRequest och en HandlerFunction (nästa steg i kedjan). Den måste returnera en ServerResponse.
//                .filter((request, next) -> {
//                    // Hämta användarnamnet från Principal (Spring Security)
//                    String username = request.servletRequest().getUserPrincipal() != null
//                            ? request.servletRequest().getUserPrincipal().getName()
//                            : "anonymous";
//                    //Om du använder OAuth2/OIDC kan du casta Principal till OAuth2AuthenticationToken för att komma åt mer information än bara användarnamnet
////                    if (request.servletRequest().getUserPrincipal() instanceof OAuth2AuthenticationToken auth) {
////                        String email = auth.getPrincipal().getAttribute("email");
////                        // ... lägg till i header
////                    }
//
//                    ServerRequest modifiedRequest = ServerRequest.from(request)
//                            .headers(httpHeaders -> {
//                                // .set ser till att eventuella headers från klienten raderas
//                                // och ersätts helt av gatewayens verifierade användarnamn.
//                                httpHeaders.set("X-User-Name", username);
//                            })
//                            .build();
//                    return next.handle(modifiedRequest);
//                })
//                //Alternative implementation
//            //    .before(removeRequestHeader("X-User-Name")) // Bort med klientens skräp
//            //    .before(addRequestHeader("X-User-Name", "dynamiskt-värde")) // In med korrekt info
//                .build();
//    }


}
