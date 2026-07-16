#!/usr/bin/env bash
set -euo pipefail

grafana_url="${GRAFANA_URL:-http://localhost:3000}"
service_name="${OTEL_SERVICE_NAME:-digital-worker}"
lookback_seconds="${LOOKBACK_SECONDS:-900}"
require_token_usage="${REQUIRE_TOKEN_USAGE:-false}"

require() {
  command -v "$1" >/dev/null || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

require curl
require jq

datasources="$(curl -fsS "$grafana_url/api/datasources")"
prometheus_uid="$(jq -er '[.[] | select(.type == "prometheus") | .uid] | first' <<<"$datasources")"
loki_uid="$(jq -er '[.[] | select(.type == "loki") | .uid] | first' <<<"$datasources")"
tempo_uid="$(jq -er '[.[] | select(.type == "tempo") | .uid] | first' <<<"$datasources")"

end="$(date +%s)"
start="$((end - lookback_seconds))"
log_query="{service_name=\"$service_name\"} |= \"Plan completed\""
log_response="$(curl -fsSG \
  "$grafana_url/api/datasources/proxy/uid/$loki_uid/loki/api/v1/query_range" \
  --data-urlencode "query=$log_query" \
  --data-urlencode "start=${start}000000000" \
  --data-urlencode "end=${end}000000000" \
  --data-urlencode "limit=20")"
latest_run="$(jq -cer \
  '[.data.result[] | {
      trace_id: (.stream.trace_id // .stream.traceId),
      instance: .stream.service_instance_id,
      timestamp: (.values[0][0] | tonumber)
    } | select(.trace_id != null and .instance != null)] | max_by(.timestamp)' \
  <<<"$log_response")"
trace_id="$(jq -er '.trace_id' <<<"$latest_run")"
instance_id="$(jq -er '.instance' <<<"$latest_run")"

metric_query="digital_worker_plan_milliseconds_count{service_name=\"$service_name\",instance=\"$instance_id\"}"
metric_response="$(curl -fsSG \
  "$grafana_url/api/datasources/proxy/uid/$prometheus_uid/api/v1/query" \
  --data-urlencode "query=$metric_query")"
metric_count="$(jq -er '.data.result | first | .value[1]' <<<"$metric_response")"
jq -en --arg count "$metric_count" '$count | tonumber > 0' >/dev/null

jvm_query="jvm_threads_live{service_name=\"$service_name\",instance=\"$instance_id\"}"
jvm_response="$(curl -fsSG \
  "$grafana_url/api/datasources/proxy/uid/$prometheus_uid/api/v1/query" \
  --data-urlencode "query=$jvm_query")"
jvm_threads="$(jq -er '.data.result | first | .value[1]' <<<"$jvm_response")"
jq -en --arg count "$jvm_threads" '$count | tonumber > 0' >/dev/null

token_query="sum by(gen_ai_token_type) (gen_ai_client_token_usage_total{service_name=\"$service_name\",instance=\"$instance_id\"})"
token_response="$(curl -fsSG \
  "$grafana_url/api/datasources/proxy/uid/$prometheus_uid/api/v1/query" \
  --data-urlencode "query=$token_query")"
input_tokens="$(jq -r \
  '[.data.result[] | select(.metric.gen_ai_token_type == "input") | .value[1]] | first // "0"' \
  <<<"$token_response")"
output_tokens="$(jq -r \
  '[.data.result[] | select(.metric.gen_ai_token_type == "output") | .value[1]] | first // "0"' \
  <<<"$token_response")"
total_tokens="$(jq -r \
  '[.data.result[] | select(.metric.gen_ai_token_type == "total") | .value[1]] | first // "0"' \
  <<<"$token_response")"

trace_response="$(curl -fsS \
  "$grafana_url/api/datasources/proxy/uid/$tempo_uid/api/traces/$trace_id")"
span_count="$(jq -er '[.batches[].scopeSpans[].spans[]] | length' <<<"$trace_response")"
action_span_count="$(jq -er \
  '[.batches[].scopeSpans[].spans[] | select(.name | startswith("action "))] | length' \
  <<<"$trace_response")"
jq -en --arg count "$span_count" '$count | tonumber > 0' >/dev/null
jq -en --arg count "$action_span_count" '$count | tonumber > 0' >/dev/null

echo "metrics: OK (plan_count=$metric_count, jvm_threads_live=$jvm_threads, instance=$instance_id)"
if jq -en --arg count "$total_tokens" '$count | tonumber > 0' >/dev/null; then
  echo "tokens:  OK (input=$input_tokens, output=$output_tokens, total=$total_tokens)"
elif [[ "$require_token_usage" == "true" ]]; then
  echo "tokens:  MISSING (run a successful model-backed incident first)" >&2
  exit 1
else
  echo "tokens:  SKIPPED (successful model response required; set REQUIRE_TOKEN_USAGE=true to enforce)"
fi
echo "logs:    OK (Plan completed, trace_id=$trace_id)"
echo "traces:  OK ($span_count spans, $action_span_count action spans)"
