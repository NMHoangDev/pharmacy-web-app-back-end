# Redis Cache Layer Guidelines

## Cache Aside abstraction
- Generic helper: `getOrSetCache(String key, long ttlSeconds, Supplier<T> dbCall)`
- Cache hit: return cached value
- Cache miss: execute `dbCall`, cache non-null result, return value
- Redis serializer: `GenericJackson2JsonRedisSerializer`

## Strict key format
`{service}:{entity}:{type}:{identifier}`

## Service key examples
| Service | Entity | Example Key |
|---|---|---|
| catalog-service | product detail | `catalog:product:detail:{id}` |
| catalog-service | product list | `catalog:product:list:page:{page}:size:{size}:q:{q}:category:{categoryId}:branch:{branchId}` |
| catalog-service | category list | `catalog:category:list:active` |
| inventory-service | item detail | `inventory:item:detail:{id}` |
| inventory-service | item availability | `inventory:item:availability:product:{productId}:branch:{branchId}` |
| content-service | post detail | `content:post:detail:slug:{slug}:admin:{isAdmin}` |
| content-service | post list | `content:post:list:page:{page}:size:{size}:q:{q}` |
| review-service | review list by product | `review:review:list:product:{productId}:page:{page}:size:{size}` |
| review-service | review summary | `review:review:summary:product:{productId}` |
| branch-service | branch detail | `branch:branch:detail:{id}` |
| branch-service | branch list | `branch:branch:list:status:{status}` |
| user-service | profile detail | `user:profile:detail:{id}` |
| user-service | profile list | `user:profile:list` |
| pharmacist-service | pharmacist detail | `pharmacist:pharmacist:detail:{id}` |
| pharmacist-service | pharmacist list | `pharmacist:pharmacist:list:page:{page}:size:{size}` |

## Invalidation strategy
- Delete single key: `deleteKey(key)`
- Delete by pattern with SCAN: `deleteByPattern(pattern)`
- Generic invalidation: `invalidateEntityCache(prefix, id)`

Examples:
- Product create: delete `catalog:product:list:*`
- Product update: delete detail and list
- Product delete: delete detail and related lists

## Distributed invalidation via Kafka
Topic: `cache.invalidation.events`

Payload:
```json
{
  "eventType": "product.updated",
  "entityId": "123",
  "service": "catalog"
}
```

Flow:
1. catalog-service writes DB
2. catalog-service publishes event to `cache.invalidation.events`
3. review-service consumes event
4. review-service invalidates `review:review:list:product:{id}:*` and `review:review:summary:product:{id}`

## TTL recommendation
- Default: 600 seconds
- List/search endpoints: 300-600 seconds
- Detail endpoints: 600-1800 seconds

## Reliability rules
- Use SCAN instead of KEYS
- Never fail DB writes because Redis or Kafka is unavailable
- Log cache hit/miss and invalidation failures
