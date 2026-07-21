local key = KEYS[1];
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])
local last_tokens = tonumber(redis.call('HGET', key, "tokens"))
local last_refill = tonumber(redis.call('HGET', key, "last_refill"))
if last_tokens == nil then
    last_tokens = capacity
    last_refill = now
else
    local delta = math.max(0, now - last_refill)
    local refilled = delta * refill_rate
    last_tokens = math.min(capacity, last_tokens + refilled)
    last_refill = now
end
if last_tokens >= requested then
    last_tokens = last_tokens - requested
    redis.call('HSET', key, 'tokens', last_tokens, 'last_refill', last_refill)
    redis.call('EXPIRE', key, 60)
    return {1, math.floor(last_tokens)} -- Allowed (1 = false), remaining tokens
else
    redis.call('HSET', key, 'tokens', last_tokens, 'last_refill', last_refill)
    redis.call('EXPIRE', key, 60)
    return {0, math.floor(last_tokens)} -- Denied (0 = false), remaining tokens
end