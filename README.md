# Inventory Service

Early Express 물류 플랫폼의 **재고 관리 서비스**입니다. 허브별 재고 현황 관리, 재고 예약/해제/출고 확정, 안전 재고 모니터링 등 재고 관련 모든 비즈니스 로직을 담당합니다.

---

## 📋 목차

1. [개요](#개요)
2. [기술 스택](#기술-스택)
3. [아키텍처](#아키텍처)
4. [도메인 모델](#도메인-모델)
5. [API 엔드포인트](#api-엔드포인트)
6. [Kafka 이벤트](#kafka-이벤트)
7. [환경 설정](#환경-설정)
8. [실행 방법](#실행-방법)
9. [프로젝트 구조](#프로젝트-구조)

---

## 개요

Inventory Service는 마이크로서비스 아키텍처 기반의 재고 관리 시스템으로, 다음 핵심 기능을 제공합니다:

- **재고 입출고 관리**: 재입고, 재고 조정, 출고 확정
- **재고 예약 시스템**: 주문 시 재고 예약 → 취소 시 해제 → 배송 시 확정
- **안전 재고 모니터링**: 안전 재고 이하 감지 및 알림 이벤트 발행
- **허브별 재고 관리**: 물류 허브 단위의 재고 추적
- **이벤트 기반 동기화**: Product Service와 Kafka 이벤트로 연동

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| **Framework** | Spring Boot 3.5.7 |
| **Language** | Java 21 |
| **Database** | PostgreSQL |
| **Messaging** | Apache Kafka |
| **Service Discovery** | Netflix Eureka |
| **Security** | OAuth 2.0 Resource Server (Keycloak) |
| **Build Tool** | Gradle |

---

## 아키텍처

### 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              Inventory Service                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│  Presentation Layer                                                              │
│  ┌─────────────────────┐ ┌─────────────────────┐ ┌─────────────────────────┐    │
│  │ ProducerController  │ │  AdminController    │ │  InternalController     │    │
│  │ /web/producer       │ │  /web/admin         │ │  /internal              │    │
│  └─────────────────────┘ └─────────────────────┘ └─────────────────────────┘    │
├─────────────────────────────────────────────────────────────────────────────────┤
│  Application Layer                                                               │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                         InventoryService                                  │   │
│  │  • restock()          • reserve()           • confirmShipment()          │   │
│  │  • adjustInventory()  • releaseReservation() • checkAvailability()       │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────────────────┤
│  Domain Layer                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │  Inventory (Aggregate Root)                                              │    │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐          │    │
│  │  │  StockQuantity  │  │  StockQuantity  │  │  StockQuantity  │          │    │
│  │  │  (quantityInHub)│  │  (reserved)     │  │  (safetyStock)  │          │    │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘          │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────────────────────────┤
│  Infrastructure Layer                                                            │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐              │
│  │    PostgreSQL    │  │  Kafka Producer  │  │  Kafka Consumer  │              │
│  │    Repository    │  │  (6 토픽 발행)    │  │  (2 토픽 수신)   │              │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘              │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 서비스 연동 구조

```
┌─────────────────┐                      ┌─────────────────┐
│  Product        │  ───── Kafka ─────▶  │  Inventory      │
│  Service        │  product-created     │  Service        │
│                 │  product-deleted     │                 │
└─────────────────┘                      └─────────────────┘
                                                │
         ┌──────────────────────────────────────┤
         │                                      │
         ▼                                      ▼
┌─────────────────┐                      ┌─────────────────┐
│  Product        │  ◀─────────────────  │  Order          │
│  Service        │  inventory-low-stock │  Service        │
│                 │  inventory-restocked │                 │
└─────────────────┘                      └─────────────────┘
                                                ▲
                                                │
                    inventory-reserved ─────────┤
                    stock-decreased ────────────┤
                    stock-restored ─────────────┘
```

---

## 도메인 모델

### Inventory (Aggregate Root)

```java
public class Inventory {
    // 식별자
    private String inventoryId;          // 재고 고유 ID
    private String productId;            // 상품 ID (FK)
    private String hubId;                // 허브 ID (FK)
    
    // 수량 정보 (Value Objects)
    private StockQuantity quantityInHub;     // 허브 내 전체 수량
    private StockQuantity reservedQuantity;  // 예약된 수량 (주문 처리 중)
    private StockQuantity safetyStock;       // 안전 재고
    private StockQuantity reorderPoint;      // 재주문 시점
    
    // 위치 및 시간
    private String location;             // 허브 내 물리적 위치 (형식: A-1-3)
    private LocalDateTime lastRestockedAt;  // 마지막 입고 시간
    
    // 낙관적 락
    private Long version;
    
    // Audit 필드
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private LocalDateTime deletedAt;
    private String deletedBy;
    private boolean isDeleted;
}
```

### StockQuantity (Value Object)

불변 객체로 설계되어 수량의 유효성을 보장합니다.

```java
public class StockQuantity {
    private Integer value;
    
    // 팩토리 메서드
    public static StockQuantity of(Integer value);
    public static StockQuantity zero();
    
    // 연산
    public StockQuantity increase(Integer amount);
    public StockQuantity decrease(Integer amount);  // 음수 결과 시 예외
    
    // 비교
    public boolean isGreaterThan(StockQuantity other);
    public boolean isLessThanOrEqual(StockQuantity other);
    public boolean isZero();
}
```

### 재고 수량 흐름도

```
┌─────────────────────────────────────────────────────────────────────┐
│                         quantityInHub (전체 수량)                    │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                                                                │ │
│  │   ┌──────────────────────┐    ┌────────────────────────────┐  │ │
│  │   │   reservedQuantity   │    │    availableQuantity       │  │ │
│  │   │   (예약된 수량)       │    │    (판매 가능 수량)          │  │ │
│  │   │                      │    │  = 전체 - 예약              │  │ │
│  │   └──────────────────────┘    └────────────────────────────┘  │ │
│  │                                                                │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌────────────────────────┐                                        │
│  │    safetyStock         │  ← 이하로 떨어지면 LowStock 이벤트 발행  │
│  │    (안전 재고)          │                                        │
│  └────────────────────────┘                                        │
└─────────────────────────────────────────────────────────────────────┘
```

### 재고 예약 상태 흐름

```
[주문 생성]                [주문 취소]              [배송 시작]
    │                         │                        │
    ▼                         ▼                        ▼
┌─────────┐              ┌─────────┐              ┌─────────┐
│ reserve │              │ release │              │ confirm │
│  예약   │              │  해제   │              │ 출고확정 │
└────┬────┘              └────┬────┘              └────┬────┘
     │                        │                        │
     ▼                        ▼                        ▼
┌────────────────┐    ┌────────────────┐    ┌────────────────────┐
│ reservedQty +N │    │ reservedQty -N │    │ reservedQty -N     │
│ availableQty-N │    │ availableQty+N │    │ quantityInHub -N   │
└────────────────┘    └────────────────┘    └────────────────────┘
```

---

## API 엔드포인트

### Producer API (생산업체용)

**Base Path**: `/v1/inventory/web/producer`

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/restock` | 재입고 |
| `GET` | `/products/{productId}/inventories` | 내 상품의 재고 현황 (전체 허브) |
| `GET` | `/hubs/{hubId}/inventories` | 특정 허브의 내 재고 조회 |
| `PUT` | `/inventories/{inventoryId}/adjust` | 재고 조정 |
| `PUT` | `/inventories/{inventoryId}/safety-stock` | 안전 재고 설정 |
| `PUT` | `/inventories/{inventoryId}/location` | 위치 변경 |
| `GET` | `/inventories/{inventoryId}` | 재고 상세 조회 |

#### 재입고 요청/응답

```http
POST /v1/inventory/web/producer/restock
X-User-Id: seller-001
Content-Type: application/json
```

**Request**
```json
{
  "productId": "prod-001",
  "hubId": "hub-seoul-001",
  "quantity": 500
}
```

**Response (200 OK)**
```json
{
  "inventoryId": "inv-001",
  "productId": "prod-001",
  "hubId": "hub-seoul-001",
  "totalQuantity": 1500,
  "availableQuantity": 1200,
  "reservedQuantity": 300,
  "safetyStock": 100,
  "location": "A-3-5",
  "isOutOfStock": false,
  "isBelowSafetyStock": false,
  "lastRestockedAt": "2025-01-15T10:30:00",
  "createdAt": "2024-12-01T09:00:00",
  "updatedAt": "2025-01-15T10:30:00"
}
```

#### 재고 조정 요청/응답

```http
PUT /v1/inventory/web/producer/inventories/{inventoryId}/adjust
X-User-Id: seller-001
Content-Type: application/json
```

**Request**
```json
{
  "adjustmentQuantity": -50,
  "reason": "파손으로 인한 재고 차감"
}
```

**Response (200 OK)**
```json
{
  "inventoryId": "inv-001",
  "productId": "prod-001",
  "hubId": "hub-seoul-001",
  "previousQuantity": 1500,
  "adjustmentQuantity": -50,
  "currentQuantity": 1450,
  "reason": "파손으로 인한 재고 차감"
}
```

---

### Admin API (운영자용)

**Base Path**: `/v1/inventory/web/admin`

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/inventories` | 전체 재고 조회 (페이징) |
| `GET` | `/hubs/{hubId}/inventories` | 허브별 재고 현황 |
| `GET` | `/out-of-stock` | 품절 상품 목록 |
| `GET` | `/low-stock` | 안전 재고 이하 상품 목록 |
| `GET` | `/inventories/{inventoryId}` | 재고 상세 조회 |
| `GET` | `/products/{productId}/inventories` | 상품별 재고 현황 |

#### 품절 상품 조회

```http
GET /v1/inventory/web/admin/out-of-stock
```

**Response (200 OK)**
```json
[
  {
    "inventoryId": "inv-003",
    "productId": "prod-003",
    "hubId": "hub-busan-001",
    "totalQuantity": 0,
    "availableQuantity": 0,
    "reservedQuantity": 0,
    "safetyStock": 50,
    "location": "B-2-1",
    "isOutOfStock": true,
    "isBelowSafetyStock": true,
    "lastRestockedAt": "2025-01-10T14:00:00",
    "createdAt": "2024-11-01T09:00:00",
    "updatedAt": "2025-01-14T16:30:00"
  }
]
```

#### 안전 재고 이하 상품 조회

```http
GET /v1/inventory/web/admin/low-stock
```

**Response (200 OK)**
```json
[
  {
    "inventoryId": "inv-002",
    "productId": "prod-002",
    "hubId": "hub-seoul-001",
    "totalQuantity": 80,
    "availableQuantity": 30,
    "reservedQuantity": 50,
    "safetyStock": 100,
    "location": "A-1-2",
    "isOutOfStock": false,
    "isBelowSafetyStock": true,
    "lastRestockedAt": "2025-01-08T11:00:00",
    "createdAt": "2024-10-15T09:00:00",
    "updatedAt": "2025-01-14T09:00:00"
  }
]
```

---

### Internal API (서비스 간 통신)

**Base Path**: `/v1/inventory/internal`

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/products/{productId}/hubs/{hubId}/availability` | 재고 가용성 확인 |
| `POST` | `/products/check-availability` | 대량 재고 확인 |
| `POST` | `/reservations` | 재고 예약 |
| `DELETE` | `/reservations/{orderId}` | 예약 해제 |
| `POST` | `/reservations/{orderId}/confirm` | 출고 확정 |
| `GET` | `/products/{productId}/inventories` | 상품별 전체 재고 조회 |
| `POST` | `/products/{productId}/initialize` | 초기 재고 생성 |
| `GET` | `/inventories/{inventoryId}/exists` | 재고 존재 확인 |

#### 재고 가용성 확인 (단건)

```http
GET /v1/inventory/internal/products/{productId}/hubs/{hubId}/availability
```

**Response (200 OK)**
```json
{
  "productId": "prod-001",
  "hubId": "hub-seoul-001",
  "isAvailable": true,
  "availableQuantity": 1200,
  "reservedQuantity": 300,
  "totalQuantity": 1500
}
```

#### 대량 재고 확인

```http
POST /v1/inventory/internal/products/check-availability
Content-Type: application/json
```

**Request**
```json
{
  "hubId": "hub-seoul-001",
  "items": [
    { "productId": "prod-001", "quantity": 10 },
    { "productId": "prod-002", "quantity": 5 },
    { "productId": "prod-003", "quantity": 20 }
  ]
}
```

**Response (200 OK)**
```json
{
  "hubId": "hub-seoul-001",
  "allAvailable": false,
  "results": [
    {
      "productId": "prod-001",
      "requiredQuantity": 10,
      "availableQuantity": 1200,
      "isAvailable": true
    },
    {
      "productId": "prod-002",
      "requiredQuantity": 5,
      "availableQuantity": 30,
      "isAvailable": true
    },
    {
      "productId": "prod-003",
      "requiredQuantity": 20,
      "availableQuantity": 0,
      "isAvailable": false
    }
  ]
}
```

#### 재고 예약

```http
POST /v1/inventory/internal/reservations
Content-Type: application/json
```

**Request**
```json
{
  "orderId": "order-001",
  "items": [
    {
      "productId": "prod-001",
      "hubId": "hub-seoul-001",
      "quantity": 10
    },
    {
      "productId": "prod-002",
      "hubId": "hub-seoul-001",
      "quantity": 5
    }
  ]
}
```

**Response (200 OK / 206 Partial Content)**
```json
{
  "reservationId": "res-uuid-001",
  "orderId": "order-001",
  "allSuccess": true,
  "reservedItems": [
    {
      "productId": "prod-001",
      "hubId": "hub-seoul-001",
      "quantity": 10,
      "success": true,
      "errorMessage": null
    },
    {
      "productId": "prod-002",
      "hubId": "hub-seoul-001",
      "quantity": 5,
      "success": true,
      "errorMessage": null
    }
  ]
}
```

#### 예약 해제

```http
DELETE /v1/inventory/internal/reservations/{orderId}?productId=prod-001&hubId=hub-seoul-001&quantity=10
```

**Response (200 OK)**
```json
{
  "orderId": "order-001",
  "productId": "prod-001",
  "hubId": "hub-seoul-001",
  "quantity": 10,
  "released": true
}
```

#### 출고 확정

```http
POST /v1/inventory/internal/reservations/{orderId}/confirm?productId=prod-001&hubId=hub-seoul-001&quantity=10
```

**Response (200 OK)**
```json
{
  "orderId": "order-001",
  "productId": "prod-001",
  "hubId": "hub-seoul-001",
  "quantity": 10,
  "confirmed": true
}
```

#### 초기 재고 생성 (Product Service 호출)

```http
POST /v1/inventory/internal/products/{productId}/initialize
Content-Type: application/json
```

**Request**
```json
{
  "productId": "prod-001",
  "sellerId": "seller-001"
}
```

**Response (201 Created)**
```json
{
  "productId": "prod-001",
  "inventories": [
    {
      "inventoryId": "inv-001",
      "hubId": "hub-seoul-001",
      "totalQuantity": 0
    },
    {
      "inventoryId": "inv-002",
      "hubId": "hub-busan-001",
      "totalQuantity": 0
    }
  ]
}
```

---

## Kafka 이벤트

### 이벤트 흐름도

```
                              Inventory Service
                    ┌────────────────────────────────────┐
                    │                                    │
  Product Service   │                                    │   Order Service
  ┌───────────┐     │    ┌────────────────────────┐     │     ┌───────────┐
  │           │─────┼───▶│   ProductEventConsumer │     │     │           │
  │ product-  │     │    │                        │     │     │           │
  │ created   │     │    │ • createInitialInventory()   │     │           │
  │           │     │    └────────────────────────┘     │     │           │
  └───────────┘     │                                    │     │           │
                    │                                    │     │           │
  ┌───────────┐     │    ┌────────────────────────┐     │     │           │
  │           │─────┼───▶│   ProductEventConsumer │     │     │           │
  │ product-  │     │    │                        │     │     │           │
  │ deleted   │     │    │ • deleteInventoriesByProduct()   │  │           │
  │           │     │    └────────────────────────┘     │     │           │
  └───────────┘     │                                    │     │           │
                    │                                    │     │           │
                    │    ┌────────────────────────┐     │     │           │
  ┌───────────┐     │    │ KafkaInventoryEvent    │     │     │           │
  │           │◀────┼────│ Publisher              │─────┼────▶│ inventory │
  │ inventory │     │    │                        │     │     │ -reserved │
  │ -low-stock│     │    │ 6개 토픽 발행           │     │     └───────────┘
  └───────────┘     │    └────────────────────────┘     │
                    │              │                     │     ┌───────────┐
  ┌───────────┐     │              │                     │     │           │
  │           │◀────┼──────────────┤                     ├────▶│ stock-    │
  │ inventory │     │              │                     │     │ decreased │
  │ -restocked│     │              │                     │     └───────────┘
  └───────────┘     │              │                     │
                    │              │                     │     ┌───────────┐
                    │              │                     │     │           │
                    │              └─────────────────────┼────▶│ stock-    │
                    │                                    │     │ restored  │
                    │                                    │     └───────────┘
                    └────────────────────────────────────┘
```

### 수신 이벤트 (Consumer)

#### 1. product-created (Product Service → Inventory Service)

상품 생성 시 해당 허브에 초기 재고 레코드를 생성합니다.

**토픽**: `product-created`

```json
{
  "eventId": "evt-uuid-001",
  "eventType": "PRODUCT_CREATED",
  "source": "product-service",
  "timestamp": "2025-01-15T09:00:00",
  "productId": "prod-001",
  "sellerId": "seller-001",
  "hubId": "hub-seoul-001",
  "name": "유기농 사과 1kg",
  "createdAt": "2025-01-15T09:00:00"
}
```

**처리 로직**:
```java
inventoryService.createInitialInventory(event.getProductId(), event.getHubId());
```

---

#### 2. product-deleted (Product Service → Inventory Service)

상품 삭제(단종) 시 해당 상품의 모든 재고를 소프트 삭제합니다.

**토픽**: `product-deleted`

```json
{
  "eventId": "evt-uuid-002",
  "eventType": "PRODUCT_DELETED",
  "source": "product-service",
  "timestamp": "2025-01-15T10:00:00",
  "productId": "prod-001",
  "sellerId": "seller-001",
  "deletedAt": "2025-01-15T10:00:00"
}
```

**처리 로직**:
```java
inventoryService.deleteInventoriesByProduct(event.getProductId());
```

---

### 발행 이벤트 (Producer)

#### 1. inventory-created

재고 레코드가 새로 생성되었을 때 발행됩니다.

**토픽**: `inventory-created`

```json
{
  "eventId": "evt-uuid-003",
  "eventType": "INVENTORY_CREATED",
  "source": "inventory-service",
  "timestamp": "2025-01-15T09:00:05",
  "inventoryId": "inv-001",
  "productId": "prod-001",
  "hubId": "hub-seoul-001",
  "quantity": 0,
  "createdAt": "2025-01-15T09:00:05"
}
```

---

#### 2. inventory-low-stock (→ Product Service)

가용 재고가 안전 재고 이하로 떨어졌을 때 발행됩니다.

**토픽**: `inventory-low-stock`

```json
{
  "eventId": "evt-uuid-004",
  "eventType": "INVENTORY_LOW_STOCK",
  "source": "inventory-service",
  "timestamp": "2025-01-15T14:30:00",
  "inventoryId": "inv-001",
  "productId": "prod-001",
  "hubId": "hub-seoul-001",
  "currentQuantity": 45,
  "safetyStock": 100,
  "detectedAt": "2025-01-15T14:30:00"
}
```

**Product Service 처리**: `markAsOutOfStock()` 호출 (상품 품절 상태로 변경)

---

#### 3. inventory-restocked (→ Product Service)

재입고가 완료되었을 때 발행됩니다.

**토픽**: `inventory-restocked`

```json
{
  "eventId": "evt-uuid-005",
  "eventType": "INVENTORY_RESTOCKED",
  "source": "inventory-service",
  "timestamp": "2025-01-15T10:30:00",
  "inventoryId": "inv-001",
  "productId": "prod-001",
  "hubId": "hub-seoul-001",
  "restockedQuantity": 500,
  "currentQuantity": 1500,
  "restockedAt": "2025-01-15T10:30:00"
}
```

**Product Service 처리**: `restoreFromOutOfStock()` 호출 (품절 해제)

---

#### 4. inventory-reserved (→ Order Service)

주문에 대한 재고 예약이 완료되었을 때 발행됩니다.

**토픽**: `inventory-reserved`
**메시지 키**: `orderId` (Order Service 파티셔닝)

```json
{
  "eventId": "evt-uuid-006",
  "eventType": "INVENTORY_RESERVED",
  "source": "inventory-service",
  "timestamp": "2025-01-15T11:00:00",
  "inventoryId": "inv-001",
  "productId": "prod-001",
  "hubId": "hub-seoul-001",
  "orderId": "order-001",
  "reservedQuantity": 10,
  "availableQuantity": 1190,
  "reservedAt": "2025-01-15T11:00:00"
}
```

---

#### 5. stock-decreased (→ Order Service)

출고 확정으로 실제 재고가 차감되었을 때 발행됩니다.

**토픽**: `stock-decreased`
**메시지 키**: `orderId`

```json
{
  "eventId": "evt-uuid-007",
  "eventType": "STOCK_DECREASED",
  "source": "inventory-service",
  "timestamp": "2025-01-15T12:00:00",
  "inventoryId": "inv-001",
  "productId": "prod-001",
  "hubId": "hub-seoul-001",
  "orderId": "order-001",
  "decreasedQuantity": 10,
  "remainingQuantity": 1490,
  "decreasedAt": "2025-01-15T12:00:00"
}
```

---

#### 6. stock-restored (→ Order Service)

주문 취소로 예약된 재고가 복원되었을 때 발행됩니다.

**토픽**: `stock-restored`
**메시지 키**: `orderId`

```json
{
  "eventId": "evt-uuid-008",
  "eventType": "STOCK_RESTORED",
  "source": "inventory-service",
  "timestamp": "2025-01-15T13:00:00",
  "inventoryId": "inv-001",
  "productId": "prod-001",
  "hubId": "hub-seoul-001",
  "orderId": "order-001",
  "restoredQuantity": 10,
  "currentQuantity": 1200,
  "restoredAt": "2025-01-15T13:00:00"
}
```

---

## 환경 설정

### 환경 변수 (.env)

```properties
# 서버 설정
APP_PORT=4015

# 데이터베이스
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/inventory_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=password

# Eureka
EUREKA_DEFAULT_ZONE=http://localhost:8761/eureka/

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Keycloak
KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/early-express

# Kafka Topics
KAFKA_TOPIC_PRODUCT_CREATED=product-created
KAFKA_TOPIC_PRODUCT_DELETED=product-deleted
KAFKA_TOPIC_INVENTORY_CREATED=inventory-created
KAFKA_TOPIC_INVENTORY_LOW_STOCK=inventory-low-stock
KAFKA_TOPIC_INVENTORY_RESTOCKED=inventory-restocked
KAFKA_TOPIC_INVENTORY_RESERVED=inventory-reserved
KAFKA_TOPIC_STOCK_DECREASED=stock-decreased
KAFKA_TOPIC_STOCK_RESTORED=stock-restored
```

### application.yml 주요 설정

```yaml
spring:
  application:
    name: inventory-service
  
  kafka:
    consumer:
      group-id: inventory-service-group
      enable-auto-commit: false  # 수동 ACK
      auto-offset-reset: earliest
    producer:
      acks: all

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_DEFAULT_ZONE}
```

---

## 실행 방법

### 사전 요구사항

- Java 21
- PostgreSQL 15+
- Apache Kafka
- Eureka Server
- Keycloak

### 로컬 실행

```bash
# 1. 데이터베이스 생성
createdb inventory_db

# 2. 환경 변수 설정
export $(cat .env | xargs)

# 3. 애플리케이션 실행
./gradlew bootRun
```

### Docker 실행

```bash
docker-compose up -d inventory-service
```

### Health Check

```bash
curl http://localhost:4015/actuator/health
```

---

## 프로젝트 구조

```
src/main/java/com/early_express/inventory_service/
├── domain/inventory/
│   ├── application/
│   │   ├── dto/
│   │   │   ├── command/          # RestockCommand, AdjustCommand, ReservationCommand 등
│   │   │   └── result/           # AvailabilityInfo, ReservationInfo 등
│   │   └── service/
│   │       └── InventoryService.java
│   │
│   ├── domain/
│   │   ├── exception/
│   │   │   ├── InventoryErrorCode.java
│   │   │   └── InventoryException.java
│   │   ├── messaging/
│   │   │   ├── InventoryEventPublisher.java        # 도메인 인터페이스 (Port)
│   │   │   └── dto/                                # EventData DTOs
│   │   │       ├── InventoryCreatedEventData.java
│   │   │       ├── InventoryLowStockEventData.java
│   │   │       ├── InventoryRestockedEventData.java
│   │   │       ├── InventoryReservedEventData.java
│   │   │       ├── StockDecreasedEventData.java
│   │   │       └── StockRestoredEventData.java
│   │   ├── model/
│   │   │   ├── Inventory.java                      # Aggregate Root
│   │   │   └── vo/
│   │   │       └── StockQuantity.java              # Value Object
│   │   └── repository/
│   │       └── InventoryRepository.java
│   │
│   ├── infrastructure/
│   │   ├── messaging/
│   │   │   ├── inventory/
│   │   │   │   ├── event/                          # Kafka 이벤트 클래스
│   │   │   │   │   ├── InventoryCreatedEvent.java
│   │   │   │   │   ├── InventoryLowStockEvent.java
│   │   │   │   │   ├── InventoryRestockedEvent.java
│   │   │   │   │   ├── InventoryReservedEvent.java
│   │   │   │   │   ├── StockDecreasedEvent.java
│   │   │   │   │   └── StockRestoredEvent.java
│   │   │   │   └── producer/
│   │   │   │       └── KafkaInventoryEventPublisher.java  # Adapter
│   │   │   └── product/
│   │   │       ├── consumer/
│   │   │       │   └── ProductEventConsumer.java
│   │   │       └── event/
│   │   │           ├── ProductCreatedEvent.java
│   │   │           └── ProductDeletedEvent.java
│   │   └── persistence/
│   │       ├── InventoryEntity.java
│   │       ├── InventoryJpaRepository.java
│   │       └── InventoryRepositoryImpl.java
│   │
│   └── presentation/
│       ├── internal/
│       │   ├── InternalInventoryController.java
│       │   └── dto/
│       │       ├── request/
│       │       │   ├── CheckAvailabilityRequest.java
│       │       │   ├── InitializeInventoryRequest.java
│       │       │   └── ReserveStockRequest.java
│       │       └── response/
│       │           ├── AvailabilityResponse.java
│       │           ├── BulkAvailabilityResponse.java
│       │           ├── ConfirmResponse.java
│       │           ├── ExistsResponse.java
│       │           ├── InitializeInventoryResponse.java
│       │           ├── InternalInventoryResponse.java
│       │           ├── ReleaseResponse.java
│       │           └── ReservationResponse.java
│       └── web/
│           ├── AdminInventoryController.java
│           ├── ProducerInventoryController.java
│           └── dto/
│               ├── request/
│               │   ├── AdjustInventoryRequest.java
│               │   ├── RestockRequest.java
│               │   ├── UpdateLocationRequest.java
│               │   └── UpdateSafetyStockRequest.java
│               └── response/
│                   ├── AdjustmentResponse.java
│                   └── InventoryResponse.java
│
└── global/
    ├── common/
    │   └── utils/
    │       └── PageUtils.java
    ├── infrastructure/
    │   └── event/
    │       └── base/
    │           └── BaseEvent.java
    └── presentation/
        └── dto/
            └── PageResponse.java
```

---

## 보안

- **OAuth 2.0 Resource Server**: Keycloak JWT 토큰 검증
- **역할 기반 접근 제어**:
    - Producer API: `X-User-Id` 헤더로 판매자 식별
    - Admin API: ADMIN 권한 필요 (TODO)
    - Internal API: 서비스 간 통신 (인증 우회 또는 서비스 토큰)
- **낙관적 락**: `@Version` 필드로 동시성 제어

---

## 모니터링

- **Actuator**: `/actuator/health`, `/actuator/info`
- **Zipkin**: 분산 추적
- **Loki**: 로그 수집
- **Prometheus Pushgateway**: 메트릭 수집

---

## 관련 서비스

| 서비스 | 연동 방식 | 역할 |
|--------|----------|------|
| **Product Service** | Kafka (양방향) | 상품 생성/삭제 이벤트 수신, 재고 상태 이벤트 발행 |
| **Order Service** | Internal API + Kafka | 재고 예약/해제/확정 API, 재고 이벤트 발행 |
| **Hub Service** | 참조 | 허브 ID 참조 |

---

## TODO

- [ ] Admin 권한 검증 구현
- [ ] 재고 이동 (허브 간) 기능: `POST /admin/inventories/transfer`
- [ ] 재고 이력 조회 기능
- [ ] 대량 재입고 API
- [ ] 재고 알림 설정 (이메일/Slack)