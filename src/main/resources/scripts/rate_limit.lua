local key = KEYS[1]

local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])

local window_start = now - window

-- Remove expired requests
redis.call('ZREMRANGEBYSCORE', key, 0, window_start)

-- Count requests in the current window
local count = redis.call('ZCARD', key)

if count >= limit then
    local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')

    if oldest[2] then
        local reset = tonumber(oldest[2]) + window - now
        return {0, limit, 0, math.max(reset, 0)}
    end

    return {0, limit, 0, window}
end

-- Add current request
redis.call('ZADD', key, now, tostring(now) .. '-' .. tostring(math.random()))

-- Set expiration
redis.call('PEXPIRE', key, window)

local new_count = count + 1
local remaining = limit - new_count

return {1, limit, remaining, window}