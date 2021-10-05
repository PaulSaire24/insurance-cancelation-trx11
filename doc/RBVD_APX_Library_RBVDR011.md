# ![Logo-template](images/logo-template.png)
# Library RBVDR011

> El objetivo de este documento es proveer información relacionada a la librería RBVDR011, la cual es utilizada por la transacción RBVDT011 y que ha sido implementada en APX.

### 1. Funcionalidad:

> Esta Librería APX tiene como objetivo realizar la lógica de negocio de la transacción RBVDT011.

#### 1.1 Caso de Uso:

> El uso de la Librería RBVDR011 está orientado a realizar los mapeos de los campos de salida de la transacción y todo lo necesario para cumplir con la lógica de negocio.

### 2. Capacidades:

> Esta **librería** brinda la capacidad de poder ejecutar la lógica de negocio de la transacción de registrar como cancelada una póliza (RBVDT011) de forma fácil y segura con el siguiente método:

#### 2.1 Método 1: EntityOutPolicyCancellationDTO executePolicyCancellation(InputParametersPolicyCancellationDTO input)
> Método que ejecuta toda la lógica de negocio

##### 2.1.1 Datos de Entrada

|#|Nombre del Atributo|Tipo de Dato| Descripción|
| :----|:---------- |:--------------| :-----|
|1| input | Object | Objeto que contiene los datos de entrada |
|1.1| contractId | String | Cadena que representa en número de contrato de la póliza |
|1.2| cancellationDate | Date | Fecha de cancelación de la póliza |
|1.3| reason | Objeto | Objeto que contiene información de la razón de la cancelación |
|1.3.1| id | String | Identificador de la razón por la que se ejecuta la cancelación |
|1.4| notifications | Objeto | Objeto que contiene información de contacto |
|1.4.1| contactDetails | List<ContactDetailDTO> | Lista de datos de contacto |
|1.4.1.1| contact | Object | Objeto que contiene la información de contacto |
|1.4.1.1.1| contactDetailType | String | Tipo de contacto de línea fija |
|1.4.1.1.2| number | String | Número de línea fija |
|1.4.1.1.3| address | String | Correo del cliente para cancelación de póliza |
|1.4.1.1.4| username | String | Nombre de usuario en la red social |
|1.5| traceId | String | Cadena que representa el identificador del flujo en marcha |
|1.6| branchId | String | Cadena que representa el identificador de la oficina |
|1.7| channelId | String | Cadena que representa el identificador del canal |
|1.8| transactionId | String | Cadena que representa el identificador de la transacción |
|1.9| userId | String | Cadena que representa el identificador del usuarió que realizó la consulta |

##### 2.1.2 Datos de Salida

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


##### 2.1.3 Ejemplo
```java
EntityOutPolicyCancellationDTO out = RBVDR011.executePolicyCancellation(InputParametersPolicyCancellationDTO input);
```

### 3.  Mensajes:

No se requieren mensajes.

### 4.  Versiones:
#### 4.1  Versión 0.4.2

+ Versión 0.4.2: Esta versión permite realizar la lógica de negocio para cumplir con el proceso deseado de la transaccion RBVDT011.