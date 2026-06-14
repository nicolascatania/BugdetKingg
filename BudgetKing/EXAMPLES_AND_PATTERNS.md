# Ejemplos de Tests para otros Controladores

## Patrón General para WebMvcTest

```java
@WebMvcTest(TuControlador.class)
@Import(TestSecurityConfig.class)
class TuControladorWebMvcTest {

    @Autowired
    private WebApplicationContext context;

    private RestTestClient client;

    @MockBean
    private TuServicio tuServicio;

    @BeforeEach
    void setup() {
        this.client = RestTestClient.bindToApplicationContext(context).build();
    }

    @Test
    void nombreDelTest_DebeValidarComportamientoEspecifico() {
        // Given - Configurar datos y comportamiento esperado
        var expectedData = new TuDTO(...);
        when(tuServicio.obtener()).thenReturn(expectedData);

        // When - Ejecutar la solicitud
        var responseBody = this.client.get()
                .uri("/endpoint-path")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TuDTO.class)
                .returnResult()
                .getResponseBody();

        // Then - Verificar los resultados
        assertNotNull(responseBody);
        assertEquals(expectedData.getId(), responseBody.getId());
    }
}
```

---

## Ejemplo: TransactionController Test

```java
@WebMvcTest(TransactionController.class)
@Import(TestSecurityConfig.class)
class TransactionControllerWebMvcTest {

    @Autowired
    private WebApplicationContext context;

    private RestTestClient client;

    @MockBean
    private TransactionService transactionService;

    @BeforeEach
    void setup() {
        this.client = RestTestClient.bindToApplicationContext(context).build();
    }

    @Test
    void crearTransaccion_DebeRetornar201_CuandoLaDatosValidos() {
        // Given
        var nuevoDTO = new TransactionDTO();
        nuevoDTO.setMonto(BigDecimal.valueOf(100.0));
        nuevoDTO.setDescripcion("Compra en mercado");
        nuevoDTO.setTipo(TransactionType.EXPENSE);

        var respuestaEsperada = new TransactionDTO();
        respuestaEsperada.setId(UUID.randomUUID());
        respuestaEsperada.setMonto(BigDecimal.valueOf(100.0));

        when(transactionService.crear(any(TransactionDTO.class)))
                .thenReturn(respuestaEsperada);

        // When & Then
        this.client.post()
                .uri("/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .body(nuevoDTO)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION);
    }

    @Test
    void obtenerTransaccionesDeMes_DebeRetornar200_ConLista() {
        // Given
        var mes = 6;
        var año = 2026;
        var transacciones = List.of(
                new TransactionDTO(UUID.randomUUID(), BigDecimal.valueOf(100.0), TransactionType.EXPENSE),
                new TransactionDTO(UUID.randomUUID(), BigDecimal.valueOf(200.0), TransactionType.INCOME)
        );

        when(transactionService.obtenerPorMes(mes, año))
                .thenReturn(transacciones);

        // When & Then
        this.client.get()
                .uri("/transaction/mes/{mes}/{ano}", mes, año)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<TransactionDTO>>() {})
                .isEqualTo(transacciones);
    }

    @Test
    void actualizarTransaccion_DebeRetornar400_CuandoIdNoExiste() {
        // Given
        var idInexistente = UUID.randomUUID();
        var updateDTO = new TransactionDTO();

        when(transactionService.actualizar(eq(idInexistente), any()))
                .thenThrow(new EntityNotFoundException("Transaction not found"));

        // When & Then
        this.client.put()
                .uri("/transaction/{id}", idInexistente)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDTO)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void eliminarTransaccion_DebeRetornar204_CuandoEliminacionExitosa() {
        // Given
        var idAEliminar = UUID.randomUUID();
        doNothing().when(transactionService).eliminar(idAEliminar);

        // When & Then
        this.client.delete()
                .uri("/transaction/{id}", idAEliminar)
                .exchange()
                .expectStatus().isNoContent();

        verify(transactionService, times(1)).eliminar(idAEliminar);
    }
}
```

---

## Ejemplo: CategoryController Test

```java
@WebMvcTest(CategoryController.class)
@Import(TestSecurityConfig.class)
class CategoryControllerWebMvcTest {

    @Autowired
    private WebApplicationContext context;

    private RestTestClient client;

    @MockBean
    private CategoryService categoryService;

    @BeforeEach
    void setup() {
        this.client = RestTestClient.bindToApplicationContext(context).build();
    }

    @Test
    void obtenerCategorias_DebeRetornarListaCompleta() {
        // Given
        var categorias = List.of(
                new CategoryDTO(UUID.randomUUID(), "Alimentación", "#FF5733"),
                new CategoryDTO(UUID.randomUUID(), "Transporte", "#33B8FF")
        );

        when(categoryService.obtenerTodas()).thenReturn(categorias);

        // When & Then
        this.client.get()
                .uri("/category")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<CategoryDTO>>() {})
                .isEqualTo(categorias);
    }

    @Test
    void getOptions_DebeRetornarListaDeOptions() {
        // Given - Este es el endpoint genérico implementado en CrudController
        var options = List.of(
                new OptionDTO(UUID.randomUUID(), "Alimentación"),
                new OptionDTO(UUID.randomUUID(), "Transporte")
        );

        when(categoryService.getOptions()).thenReturn(options);

        // When & Then
        this.client.get()
                .uri("/category/options")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<OptionDTO>>() {})
                .isEqualTo(options);
    }
}
```

---

## Patrones Comunes en Mockito para Tests

### 1. Configurar comportamiento de retorno
```java
// Retorna un valor fijo
when(servicio.obtenerPorId(any())).thenReturn(nuevoDTO);

// Retorna Optional
when(servicio.buscar(eq("valor"))).thenReturn(Optional.of(dto));

// Lanza excepción
when(servicio.eliminar(any())).thenThrow(new RuntimeException("Error"));

// Retorna valores diferentes según los argumentos
when(servicio.obtener(1)).thenReturn(dto1);
when(servicio.obtener(2)).thenReturn(dto2);
```

### 2. Matchers de Mockito
```java
// Cualquier argumento del tipo especificado
when(servicio.guardar(any(TransactionDTO.class))).thenReturn(saved);

// Argumentos específicos
when(servicio.obtenerPorId(eq(idEsperado))).thenReturn(dto);

// Múltiples argumentos
when(servicio.buscar(anyString(), anyInt())).thenReturn(resultados);

// Predicados personalizados
when(servicio.buscar(argThat(s -> s.startsWith("test")))).thenReturn(dto);
```

### 3. Verificar llamadas a métodos
```java
// Verificar que se llamó exactamente una vez
verify(servicio, times(1)).guardar(any());

// Verificar que nunca se llamó
verify(servicio, never()).eliminar(any());

// Verificar que se llamó al menos una vez
verify(servicio, atLeastOnce()).obtener();

// Verificar argumentos exactos pasados
verify(servicio).guardar(argThat(dto -> dto.getId().equals(expectedId)));
```

---

## HTTP Methods y Status Codes

### GET - Obtener recursos
```java
this.client.get()
    .uri("/endpoint")
    .exchange()
    .expectStatus().isOk()  // 200 OK
```

### POST - Crear recursos
```java
this.client.post()
    .uri("/endpoint")
    .contentType(MediaType.APPLICATION_JSON)
    .body(dto)
    .exchange()
    .expectStatus().isCreated()  // 201 Created
    .expectHeader().exists(HttpHeaders.LOCATION);
```

### PUT - Actualizar recursos
```java
this.client.put()
    .uri("/endpoint/{id}", id)
    .contentType(MediaType.APPLICATION_JSON)
    .body(dto)
    .exchange()
    .expectStatus().isOk()  // 200 OK
```

### DELETE - Eliminar recursos
```java
this.client.delete()
    .uri("/endpoint/{id}", id)
    .exchange()
    .expectStatus().isNoContent()  // 204 No Content
```

### Otros Status Codes
```java
.expectStatus().isCreated()         // 201
.expectStatus().isNoContent()       // 204
.expectStatus().isBadRequest()      // 400
.expectStatus().isUnauthorized()    // 401
.expectStatus().isForbidden()       // 403
.expectStatus().isNotFound()        // 404
.expectStatus().isEqualTo(503)      // 503 (custom)
```

---

## Tips y Mejores Prácticas

### ✅ QUÉ HACER
```java
// 1. Tests descriptivos con nombres claros
@Test
void crear_DebeRetornar201_CuandoLosDataSonValidos() { }

// 2. Usar Given-When-Then para estructura clara
void test() {
    // Given - Preparación
    // When - Acción
    // Then - Verificación
}

// 3. Un comportamiento por test
@Test
void unaSoloAssercionPrincipal() { }

// 4. Usar constantes para URLs
private static final String ENDPOINT = "/category";

// 5. Mockear dependencias externas
@MockBean
private ExternalApiClient apiClient;
```

### ❌ QUÉ EVITAR
```java
// 1. Tests con nombres ambiguos
@Test
void test1() { }  // ❌ No describe qué testea

// 2. Múltiples comportamientos en un test
@Test
void testTodo() {
    // Testea GET, POST, PUT, DELETE en el mismo método
}

// 3. Tests que dependen de orden de ejecución
// (JUnit ejecuta tests en orden aleatorio por defecto)

// 4. No usar @BeforeEach para inicialización compartida
// Esto puede causar estado compartido entre tests

// 5. Ignorar campos opcionales en DTOs
var dto = new TransactionDTO();  // Faltan campos
```

---

## Ejecutar Tests desde Línea de Comandos

```bash
# Ejecutar todos los tests
mvn test

# Ejecutar una clase de test específica
mvn test -Dtest=DolarControllerWebMvcTest

# Ejecutar un test específico dentro de una clase
mvn test -Dtest=DolarControllerWebMvcTest#obtenerCotizacionOficial_DeberiaRetornar200*

# Ejecutar tests con patrón
mvn test -Dtest=*Controller*Test

# Mostrar más detalles
mvn test -X

# Ejecutar tests paralelos (más rápido)
mvn test -DparallelTestClasses=true
```

---

## Checklist para Nuevos Tests

- [ ] ¿La clase tiene `@WebMvcTest(TuControlador.class)`?
- [ ] ¿Tiene `@Import(TestSecurityConfig.class)` si usa seguridad?
- [ ] ¿RestTestClient está inicializado en `@BeforeEach`?
- [ ] ¿Todos los servicios están mockeados con `@MockBean`?
- [ ] ¿El test tiene nombre descriptivo que explique el caso?
- [ ] ¿Usa estructura Given-When-Then?
- [ ] ¿Verifica el status HTTP correcto?
- [ ] ¿Verifica el body de la respuesta si es necesario?
- [ ] ¿Se verifica que se llamaron los mocks correctamente?
- [ ] ¿El test es independiente de otros tests?

