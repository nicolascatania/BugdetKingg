# ❌ Troubleshooting - Errores Comunes y Soluciones

## Error 1: `The type RestTestClient is not accessible`

### Síntomas
```
The type RestTestClient is not accessible (restriction on required library 'org.springframework:spring-webflux')
```

### Causa
Falta la dependencia `spring-boot-starter-webflux` para que `RestTestClient` esté disponible.

### Solución
En el pom.xml, asegúrate que tengas:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```

Si aún no funciona, agrega:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Error 2: `Cannot resolve symbol @MockBean`

### Síntomas
```
Cannot resolve symbol 'MockBean'
The annotation @MockBean is not found
```

### Causa
Import incorrecto. Estás usando `@MockitoBean` o el import no es el correcto.

### Solución
```java
// ❌ INCORRECTO
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// ✅ CORRECTO - Spring Boot 4.0+
import org.springframework.boot.test.mock.mockito.MockBean;
```

Si aún tiene problemas en el IDE, intenta:
1. File → Invalidate Caches → Invalidate and Restart
2. Delete `.idea` folder y `.iml` file
3. Reimport el proyecto

---

## Error 3: `401 Unauthorized` en los tests

### Síntomas
```
Resolved [org.springframework.security.authentication.InsufficientAuthenticationException: 
Full authentication is required to access this resource]
```

### Causa
`TestSecurityConfig` no está siendo importado o no está deshabilitando la seguridad correctamente.

### Solución
1. Verifica que tengas `@Import(TestSecurityConfig.class)` en tu clase de test:
   ```java
   @WebMvcTest(DollarController.class)
   @Import(TestSecurityConfig.class)  // ✅ NECESARIO
   class DolarControllerWebMvcTest {
   ```

2. Verifica que `TestSecurityConfig` tenga `@Primary` en su bean:
   ```java
   @Bean
   @Primary  // ✅ NECESARIO
   @Order(0)
   public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
   ```

3. Verifica que deshabilita CSRF:
   ```java
   .csrf(AbstractHttpConfigurer::disable)  // ✅ NECESARIO
   ```

---

## Error 4: `The method bindToApplicationContext(WebApplicationContext) is not applicable`

### Síntomas
```
The method bindToApplicationContext(WebApplicationContext) of type RestTestClient 
is not applicable for the arguments (WebApplicationContext)
```

### Causa
`RestTestClient` no puede inicializarse con ese método en esa versión.

### Solución
Asegúrate que uses Spring Boot 4.0.1 o superior en el pom.xml:
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.1</version>
</parent>
```

Si está correcto, intenta limpiar:
```bash
mvn clean
```

---

## Error 5: `NullPointerException` al acceder a `client`

### Síntomas
```
java.lang.NullPointerException: Cannot invoke method on null object reference
at DolarControllerWebMvcTest.obtenerCotizacionOficial(DolarControllerWebMvcTest.java:45)
```

### Causa
El `@BeforeEach void setup()` no se está ejecutando.

### Solución
```java
// ✅ CORRECTO
@BeforeEach  // ✅ Import de org.junit.jupiter.api
void setup() {
    this.client = RestTestClient.bindToApplicationContext(context).build();
}

// ❌ INCORRECTO (JUnit 4)
@Before  // ❌ Es de JUnit 4
void setup() { }
```

Si usas JUnit 4, cambia a JUnit 5:
```java
import org.junit.jupiter.api.BeforeEach;  // ✅ JUnit 5
import org.junit.jupiter.api.Test;        // ✅ JUnit 5
```

---

## Error 6: `Cannot find symbol WebApplicationContext`

### Síntomas
```
Cannot find symbol 'WebApplicationContext'
class 'org.springframework.web.context.WebApplicationContext' not found
```

### Causa
Import incorrecto o falta la dependencia.

### Solución
```java
// ✅ CORRECTO
import org.springframework.web.context.WebApplicationContext;

// Dependencia necesaria (debería estar incluida en spring-boot-starter-webmvc)
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```

---

## Error 7: `java.lang.AssertionError: Status expected: <200> but was: <405>`

### Síntomas
```
java.lang.AssertionError: 
Status expected:<200>
but was:<405 Method Not Allowed>
```

### Causa
El método HTTP es incorrecto o la ruta no existe.

### Solución
Verifica:
1. El método en el controlador:
   ```java
   @GetMapping("/cotizacion-oficial")  // ✅ GET, no POST
   public ResponseEntity<DolarCompraVentaDTO> obtenerCotizacionOficial() {
   ```

2. La URI en el test:
   ```java
   this.client.get()                    // ✅ .get(), no .post()
       .uri("/dolar/cotizacion-oficial")  // ✅ Ruta correcta
       .exchange()
   ```

---

## Error 8: `403 Forbidden`

### Síntomas
```
Status expected:<200>
but was:<403 Forbidden>
```

### Causa
CSRF está habilitado en los tests.

### Solución
Verifica que tu `TestSecurityConfig` tenga:
```java
.csrf(AbstractHttpConfigurer::disable)  // ✅ DEBE estar
```

O si tienes un POST/PUT/DELETE, incluye el token CSRF (pero mejor es deshabilitarlo):
```java
.csrf(csrf -> csrf.disable())  // ✅ Alternativa
```

---

## Error 9: `The annotation @WebMvcTest must specify at least one component`

### Síntomas
```
@WebMvcTest must specify at least one component class
```

### Causa
`@WebMvcTest` sin parámetro no sabe cuál controlador testear.

### Solución
```java
// ❌ INCORRECTO
@WebMvcTest

// ✅ CORRECTO
@WebMvcTest(DollarController.class)
```

---

## Error 10: Multiple `SecurityFilterChain` beans

### Síntomas
```
No qualifying bean of type 'org.springframework.security.web.SecurityFilterChain' available:
expected single matching bean but found 2: 
- securityFilterChain
- testSecurityFilterChain
```

### Causa
Hay dos beans de `SecurityFilterChain` en el contexto.

### Solución
Asegúrate que el bean de test tiene `@Primary` y `@Order(0)`:
```java
@Bean
@Primary  // ✅ IMPORTANTE
@Order(0) // ✅ IMPORTANTE - Prioridad más alta
public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
    // ...
}
```

Y que el bean de producción NO tiene `@Primary`:
```java
@Bean
// Sin @Primary aquí
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    // ...
}
```

---

## Error 11: `Cannot load @Import` TestSecurityConfig

### Síntomas
```
Unable to import configuration class TestSecurityConfig
```

### Causa
`TestSecurityConfig` no tiene `@TestConfiguration` o no está en el classpath correcto.

### Solución
1. Verifica que la clase esté en `src/test/java`:
   ```
   ✅ src/test/java/com/veritech/BudgetKing/security/config/TestSecurityConfig.java
   ❌ src/main/java/com/veritech/BudgetKing/security/config/TestSecurityConfig.java
   ```

2. Verifica que tenga `@TestConfiguration`:
   ```java
   @TestConfiguration  // ✅ NECESARIO
   public class TestSecurityConfig {
       // ...
   }
   ```

3. Si aún no funciona, intenta con `@Configuration`:
   ```java
   @Configuration  // Alternativa temporal
   public class TestSecurityConfig {
       // ...
   }
   ```

---

## Error 12: `RestTestClient` does not support multipart

### Síntomas
```
RestTestClient does not support multipart file uploads
```

### Causa
Intentas hacer upload de archivos con `RestTestClient`.

### Solución
Para tests complejos con uploads, usa `MockMvc` en lugar de `RestTestClient`:
```java
@WebMvcTest(TuControlador.class)
class TuControladorTest {
    @Autowired
    private MockMvc mockMvc;  // Alternativa para casos complejos
    
    // Para multipart:
    // mockMvc.perform(multipart("/endpoint"))
}
```

---

## Checklist de Debugging

Si tu test no funciona, verifica en orden:

- [ ] ¿Está en `src/test/java`?
- [ ] ¿Tiene `@WebMvcTest(YourController.class)`?
- [ ] ¿Tiene `@Import(TestSecurityConfig.class)`?
- [ ] ¿RestTestClient se inicializa en `@BeforeEach`?
- [ ] ¿Los imports están de `org.junit.jupiter`?
- [ ] ¿Todos los mocks tienen `@MockBean`?
- [ ] ¿El pom.xml tiene Spring Boot 4.0.1?
- [ ] ¿`TestSecurityConfig` está en `src/test/java`?
- [ ] ¿`TestSecurityConfig` tiene `@TestConfiguration`?
- [ ] ¿TestSecurityConfig.bean tiene `@Primary` y `@Order(0)`?
- [ ] ¿CSRF está deshabilitado en TestSecurityConfig?
- [ ] ¿La URL en el test coincide con el controlador?
- [ ] ¿El método HTTP (GET/POST/PUT/DELETE) es correcto?

---

## Ejecutar Tests con Debug

```bash
# Ejecutar con máximo verbosity
mvn test -X -Dtest=DolarControllerWebMvcTest

# Ejecutar un test específico
mvn test -Dtest=DolarControllerWebMvcTest#testName

# Generar reporte de cobertura
mvn test jacoco:report
```

---

## Logs Útiles

Si necesitas ver qué está pasando, agrega logs:

```java
@BeforeEach
void setup() {
    System.out.println("=== Initializing RestTestClient ===");
    this.client = RestTestClient.bindToApplicationContext(context).build();
    System.out.println("=== RestTestClient initialized ===");
}

@Test
void test() {
    System.out.println("=== Starting test ===");
    var response = this.client.get()
        .uri("/dolar/cotizacion-oficial")
        .exchange();
    System.out.println("=== Got response ===");
}
```

O usa un logger real:
```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
class DolarControllerWebMvcTest {
    // ...
    log.info("Test started");
}
```

---

## ¿Necesitas ayuda?

Si persiste el error:
1. Copia el stack trace completo
2. Verifica que sigues exactamente el patrón en `DolarControllerWebMvcTest.java`
3. Compara tu código línea por línea con los archivos corregidos
4. Limpia y reconstruye el proyecto: `mvn clean install`

**¡No des por vencido! Los tests Spring se vuelven más fáciles con la práctica.** 🚀

