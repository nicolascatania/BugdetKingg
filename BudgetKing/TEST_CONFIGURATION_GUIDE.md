# Guía de Configuración de Tests - DolarControllerWebMvcTest

## Cambios Realizados

### 1. **Archivo: `pom.xml`**
**Problema**: `spring-boot-starter-test` no tenía scope `test`

**Solución**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>  <!-- Agregado -->
</dependency>
```

**Por qué**: Esto asegura que las dependencias de test (mockito, junit, etc.) solo se incluyen en la compilación de tests y no en la aplicación de producción.

---

### 2. **Archivo: `DolarControllerWebMvcTest.java`**

#### Imports Corregidos:
```java
// ❌ Incorrecto
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// ✅ Correcto
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; // Si necesitas MockMvc
```

#### Anotaciones Correctas:
```java
@WebMvcTest(DollarController.class)  // Indica que es un test de MVC
@Import(TestSecurityConfig.class)    // Importa la config de seguridad de test
class DolarControllerWebMvcTest {
    // ...
}
```

**Por qué**:
- `@WebMvcTest`: Carga solo el contexto necesario para probar el controlador (más rápido y aislado)
- `@Import`: Inyecta la configuración de seguridad de test que deshabilita todos los filtros
- `@MockBean`: Reemplaza los beans reales por mocks en el contexto de test

#### Inicialización de RestTestClient:
```java
@Autowired
private WebApplicationContext context;

private RestTestClient client;

@BeforeEach
void setup() {
    this.client = RestTestClient.bindToApplicationContext(context).build();
}
```

**Por qué**:
- `RestTestClient` es el nuevo cliente fluido en Spring Boot 4.0
- Se debe construir a partir del `WebApplicationContext`
- El método `@BeforeEach` asegura que se inicializa antes de cada test

#### Mock del Cliente DolarApiClient:
```java
@MockBean
private DolarApiClient dolarApiClient;

// En el test:
when(dolarApiClient.obtenerCotizacionOficial()).thenReturn(Optional.of(mockApiResponse));
```

**Por qué**:
- `@MockBean` reemplaza el bean real en el contexto de Spring
- Mockito controla el comportamiento del cliente sin hacer llamadas reales a la API externa
- `when().thenReturn()` define el comportamiento esperado

---

### 3. **Archivo: `TestSecurityConfig.java`**

**Problema**: No deshabilitaba completamente los filtros de seguridad (JWT, CSRF, etc.)

**Solución**:
```java
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    @Order(0)
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)                    // Deshabilita CSRF
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())  // Permite todas las solicitudes
                .httpBasic(AbstractHttpConfigurer::disable);             // Deshabilita autenticación básica
        
        return http.build();
    }
}
```

**Por qué**:
- `@Primary` + `@Order(0)`: Asegura que esta configuración tiene prioridad sobre la de producción
- `.csrf(AbstractHttpConfigurer::disable)`: Previene errores 403 por CSRF
- `.anyRequest().permitAll()`: Evita que el JWT filter bloquee las solicitudes
- `.httpBasic(AbstractHttpConfigurer::disable)`: Deshabilita autenticación básica innecesaria en tests

---

## Qué Testea Cada Test

### Test 1: `obtenerCotizacionOficial_DeberiaRetornar200YDTO_CuandoElClienteRespondeExitosamente`
- **Escenario**: El cliente DolarApiClient devuelve una respuesta exitosa
- **Verificación**: 
  - Status 200 OK ✅
  - El DTO se transforma correctamente
  - Los valores de compra y venta son los esperados

### Test 2: `obtenerCotizacionOficial_DeberiaRetornar503_CuandoElClienteFalla`
- **Escenario**: El cliente DolarApiClient falla (devuelve Optional.empty())
- **Verificación**: 
  - Status 503 Service Unavailable
  - El controlador maneja gracefully el error sin excepciones no controladas

---

## Ventajas de Esta Configuración

### 🚀 Velocidad
- Los tests se ejecutan rápidamente porque solo cargan el contexto necesario
- No accede a base de datos real (no hay EntityManager aquí)
- No intenta conectarse a APIs externas (todo está mockeado)

### 🔒 Aislamiento
- Pruebas independientes de la seguridad real
- No requiere JWT válidos
- No interfiere con otros tests

### 📝 Legibilidad
- RestTestClient tiene una API fluida y clara
- El mock es evidente (`@MockBean`)
- Los tests leen como historias de comportamiento esperado

### 🔧 Mantenibilidad
- Cambios en SecurityConfig de producción no rompen los tests
- Los mocks son explícitos y fáciles de modificar
- Fácil agregar más tests siguiendo el mismo patrón

---

## Próximas Pasos (Recomendaciones)

1. **Agregar más tests** siguiendo este patrón para otros controladores
2. **Tests de Integración**: Crear tests con `@SpringBootTest` para probar flujos completos
3. **Tests de Repositorio**: Usar `@DataJpaTest` para probar queries personalizadas
4. **Tests de Seguridad**: Usar `@WithMockUser` para tests que requieren autenticación

---

## Referencia: RestTestClient vs MockMvc

| Característica | RestTestClient | MockMvc |
|---|---|---|
| API | Fluida, moderna | Funcional |
| Readability | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| Spring Boot | 4.0+ | Todas |
| Curva de aprendizaje | Media | Baja |
| Recomendado para | Spring Boot 4.0+ | Proyectos existentes |

**Para Spring Boot 4.0, RestTestClient es la opción recomendada** ✅

