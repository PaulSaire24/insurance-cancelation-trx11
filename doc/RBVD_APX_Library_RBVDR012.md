# ![Logo-template](images/logo-template.png)
# Library RBVDR012

> El objetivo de este documento es proveer información relacionada a la librería RBVDR012, la cual es utiliza por la librería RBVDR011 y que ha sido implementada en APX.

### 1. Funcionalidad:

> Esta Librería APX tiene como objetivo realizar consultas a servicios internos o externos requeridos por la lógica de negocio de la transacción RBVDT011.

#### 1.1 Caso de Uso:

> El uso de la Librería RBVDR012 está orientado a realizar consultas a servicios internos o externos relevantes para ejecutar una cancelación de póliza.

### 2. Capacidades:

> Esta **librería** brinda la capacidad de poder consultar servicios internos y externos de forma fácil y segura con los siguientes métodos:

#### 2.1 Método 1: PolicyCancellationPayloadBO executeCancelPolicyRimac(InputRimacBO input, PolicyCancellationPayloadBO inputPayload)
> Método que solicita a Rimac la ejecución de la cancelación de la póliza.

##### 2.1.1 Datos de Entrada

|#|Nombre del Atributo|Tipo de Dato| Descripción|
| :----|:---------- |:--------------| :-----|
|1| input | Object | Objeto que contiene a los datos de entrada |
|1.1| numeroPoliza | Integer | Valor que representa el identificador de la póliza usado por la aseguradora |
|1.2| certificado | Integer | Valor que representa el número de certificado |
|1.3| codProducto | String | Cadena que representa el código del producto de la aseguradora |
|1.4| traceId | String | Cadena que representa el identificador del flujo en marcha |
|2| inputPayload | Object | Objeto que contiene a los datos de entrada del cuerpo de la solicitud |
|2.1| poliza | Object | Objeto que contiene datos de la póliza |
|2.1.1| fechaAnulacion | String | Fecha en la que se pretende ejecutar la cancelación de la póliza |
|2.1.2| codigoMotivo | String | Código del motivo por el que se realiza la cancelación |
|2.1.3| modoDevolucion | String | Código del modo de devolución |
|2.2| contratante | Object | Objeto que contiene datos del contratante |
|2.2.1| correo | String | Fecha en la que se pretende ejecutar la cancelación de la póliza |
|2.2.2| envioElectronico | String | Le indica a Rimac si se requiere enviar constancia de la ejecución al correo proporcionado |

##### 2.1.2 Datos de Salida

|#|Nombre del Atributo|Tipo de Dato| Descripción|
| :----|:---------- |:--------------| :-----|
|1| out | Object | Objeto que contiene el mensaje de respuesta. |
|1.1| mensaje | String | Cadena que representa un mensaje de confirmación por parte de Rimac. |

##### 2.1.3 Ejemplo
```java
PolicyCancellationPayloadBO out = RBVDR012.executeCancelPolicyRimac(InputRimacBO input, PolicyCancellationPayloadBO inputPayload);
```

#### 2.2 Método 2: EntityOutPolicyCancellationDTO executeCancelPolicyHost(String contractId, Calendar cancellationDate, GenericIndicatorDTO reason, NotificationsDTO notifications)
> Método que realiza una consulta al servicio interno ASO para ejecutar una cancelación de póliza en host.

##### 2.2.1 Datos de Entrada

|#|Nombre del Atributo|Tipo de Dato| Descripción|
| :----|:---------- |:--------------| :-----|
|1| contractId | String | Cadena que representa en número de contrato de la póliza |
|2| cancellationDate | Calendar | Fecha en la que se pretende ejecutar la cancelación |
|3| reason | Objeto | Objeto que contiene información de la razón de la cancelación |
|3.1| id | String | Identificador de la razón por la que se ejecuta la cancelación |
|4| notifications | Objeto | Objeto que contiene información de contacto |
|4.1| contactDetails | List<ContactDetailDTO> | Lista de datos de contacto |
|4.1.1| contact | Object | Objeto que contiene la información de contacto |
|4.1.1.1| contactDetailType | String | Tipo de contacto de línea fija |
|4.1.1.2| number | String | Número de línea fija |
|4.1.1.3| address | String | Correo del cliente para cancelación de póliza |
|4.1.1.4| username | String | Nombre de usuario en la red social |

##### 2.2.2 Datos de Salida

|#|Nombre del Atributo|Tipo de Dato| Descripción|
| :----|:---------- |:--------------| :-----|
|1| out | Object | Objeto que contiene la respuesta de la transacción |
|1.1| id | String | Identificador de la Cancelación |
|1.2| cancellationDate | Date | Fecha de cancelación de la póliza |
|1.3| reason | Object | Objeto que contiene información de la razón de la cancelación |
|1.3.1| id | String | Identificador de la razón por la que se ejecuta la cancelación |
|1.3.2| description | String | Descripción de la razón por la que se ejecuta la cancelación |
|1.4| notifications | Object | Objeto que contiene información de contacto |
|1.4.1| contactDetails | List<ContactDetailDTO> | Lista de datos de contacto |
|1.4.1.1| contact | Object | Objeto que contiene la información de contacto |
|1.4.1.1.1| contactDetailType | String | Tipo de contacto de línea fija |
|1.4.1.1.2| number | String | Número de línea fija |
|1.4.1.1.3| address | String | Correo del cliente para cancelación de póliza |
|1.4.1.1.4| username | String | Nombre de usuario en la red social |
|1.5| status | Object | Objeto que contiene datos del estado de la póliza en la cancelación |
|1.5.1| id | String | Identificador del estado de la póliza |
|1.5.2| description | String | Descripción del estado de la póliza |
|1.6| insurerRefund | Object | Objeto que contiene datos de lo devuelto por la aseguradora |
|1.6.1| amount | Double | Monto devuelto de la aseguradora |
|1.6.2| currency | String | Moneda de devolución de la aseguradora |
|1.7| customerRefund | Object | Objeto que contiene datos del monto a devolver al cliente por el banco |
|1.7.1| amount | Double | Monto a devolver al cliente |
|1.7.2| currency | String | Moneda de devolución |
|1.8| exchangeRate | Object | Objeto que contiene datos de la conversión a devolver |
|1.8.1| value | Double | Tipo de Cambio al cobro de la cuota |
|1.8.2| baseCurrency | String | Moneda Origen |
|1.8.2| targetCurrency | String | Moneda Destino |
|1.8.2| creationDate | Date | Fecha del tipo de cambio |

##### 2.2.3 Ejemplo
```java
EntityOutPolicyCancellationDTO out = RBVDR012.executeCancelPolicyHost(String contractId, Calendar cancellationDate, GenericIndicatorDTO reason, NotificationsDTO notifications);
```

### 3.  Mensajes:
#### 3.1  Código RBVD00000102:
> Este código de error es devuelto cuando el Código de producto o Número de póliza no son válidos.

#### 3.4  Código RBVD00000107:
> Este código de error es devuelto cuando el número de contrato no es válido.

#### 3.4  Código RBVD00000126:
> Este código de error es devuelto cuando el código de producto no es un valor numérico.

#### 3.4  Código RBVD00000127:
> Este código de error es devuelto cuando el código de producto excede 4 caracteres como máximo.

#### 3.4  Código RBVD00000133:
> Este código de error es devuelto cuando el motivo de cancelación no es válido.

#### 3.4  Código RBVD00000135:
> Este código de error es devuelto cuando no se pudo conectar con el servicio de cancelación de póliza Host.

#### 3.4  Código RBVD00000139:
> Este código de error es devuelto cuando se intenta ejecutar la cancelación de una póliza que ya se encontraba anulada o cancelada.

#### 3.4  Código RBVD00000143:
> Este código de error es devuelto cuando la póliza que se intenta cancelar no existe o no está formalizada.

#### 3.4  Código RBVD00000144:
> Este código de error es devuelto cuando la fecha de cancelación excede la covertura de la póliza.

#### 3.4  Código RBVD00000145:
> Este código de error es devuelto cuando el producto no es apto para cancelación de póliza.

#### 3.4  Código RBVD00000146:
> Este código de error es devuelto cuando la póliza no se puede anular.

### 4.  Versiones:
#### 4.1  Versión 0.4.1

+ Versión 0.4.1: Esta versión permite realizar consultas a servicios internos y externos relevantes para ejecutar una cancelación de póliza.