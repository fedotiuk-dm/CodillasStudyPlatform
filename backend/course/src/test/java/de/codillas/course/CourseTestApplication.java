package de.codillas.course;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/** Context anchor so {@code @DataJpaTest} can bootstrap this library module's JPA slice. */
@SpringBootConfiguration
@EnableAutoConfiguration
class CourseTestApplication {}
