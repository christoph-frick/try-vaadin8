package app

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.support.SpringBootServletInitializer

@SpringBootApplication
class Application extends SpringBootServletInitializer {

    static void main(String[] args) throws Exception {
        def app = new SpringApplication(Application)
        // further config...
        app.run(args)
    }

}
