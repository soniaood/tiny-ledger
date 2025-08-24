#!/bin/bash

# Tiny Ledger API Test Script
# Make sure the application is running on http://localhost:8080 before executing

echo "🏦 Testing Tiny Ledger API"
echo "=========================="

# Base URL
BASE_URL="http://localhost:8080"

echo ""
echo "📊 1. Check initial balance (should be 0)"
curl -s "$BASE_URL/balance" | jq '.'

echo ""
echo "💰 2. Make initial deposit of \$50.00"
curl -s -X POST "$BASE_URL/transactions" \
  -H "Content-Type: application/json" \
  -d '{"amountInCents":5000,"type":"DEPOSIT","description":"Initial deposit"}' | jq '.'

echo ""
echo "📊 3. Check balance after deposit (should be \$50.00)"
curl -s "$BASE_URL/balance" | jq '.'

echo ""
echo "☕ 4. Make withdrawal for coffee (\$4.50)"
curl -s -X POST "$BASE_URL/transactions" \
  -H "Content-Type: application/json" \
  -d '{"amountInCents":450,"type":"WITHDRAWAL","description":"Coffee purchase"}' | jq '.'

echo ""
echo "📊 5. Check balance after withdrawal (should be \$45.50)"
curl -s "$BASE_URL/balance" | jq '.'

echo ""
echo "💼 6. Test idempotency - same transaction twice"
echo "First attempt:"
curl -s -X POST "$BASE_URL/transactions" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: salary-jan-2024" \
  -d '{"amountInCents":300000,"type":"DEPOSIT","description":"January salary"}' | jq '.'

echo ""
echo "Second attempt (should return same transaction):"
curl -s -X POST "$BASE_URL/transactions" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: salary-jan-2024" \
  -d '{"amountInCents":300000,"type":"DEPOSIT","description":"January salary"}' | jq '.'

echo ""
echo "📊 7. Check balance after salary (should be \$3045.50)"
curl -s "$BASE_URL/balance" | jq '.'

echo ""
echo "🛒 8. Make several small purchases"
curl -s -X POST "$BASE_URL/transactions" \
  -H "Content-Type: application/json" \
  -d '{"amountInCents":1250,"type":"WITHDRAWAL","description":"Lunch"}' | jq '.id, .amountInCents, .description'

curl -s -X POST "$BASE_URL/transactions" \
  -H "Content-Type: application/json" \
  -d '{"amountInCents":2500,"type":"WITHDRAWAL","description":"Gas"}' | jq '.id, .amountInCents, .description'

curl -s -X POST "$BASE_URL/transactions" \
  -H "Content-Type: application/json" \
  -d '{"amountInCents":799,"type":"WITHDRAWAL","description":"Groceries"}' | jq '.id, .amountInCents, .description'

echo ""
echo "📊 9. Final balance check (should be \$3000.01)"
curl -s "$BASE_URL/balance" | jq '.'

echo ""
echo "📋 10. View transaction history (latest 3 transactions)"
curl -s "$BASE_URL/transactions?limit=3&offset=0" | jq '.data[] | {id, type, amountInCents, description, createdOn}'

echo ""
echo "📋 11. View all transaction history"
curl -s "$BASE_URL/transactions" | jq '.data[] | {id, type, amountInCents, description}'

echo ""
echo "❌ 12. Test validation - try negative amount"
curl -s -X POST "$BASE_URL/transactions" \
  -H "Content-Type: application/json" \
  -d '{"amountInCents":-1000,"type":"WITHDRAWAL","description":"Invalid negative amount"}' | jq '.'

echo ""
echo "❌ 13. Test validation - try zero amount"
curl -s -X POST "$BASE_URL/transactions" \
  -H "Content-Type: application/json" \
  -d '{"amountInCents":0,"type":"DEPOSIT","description":"Invalid zero amount"}' | jq '.'

echo ""
echo "❌ 14. Test insufficient funds"
curl -s -X POST "$BASE_URL/transactions" \
  -H "Content-Type: application/json" \
  -d '{"amountInCents":999999,"type":"WITHDRAWAL","description":"Expensive car"}' | jq '.'

echo ""
echo "📊 15. Final balance (should be unchanged at \$3000.01)"
curl -s "$BASE_URL/balance" | jq '.'

echo ""
echo "✅ Test script completed!"
echo ""
echo "Summary of expected results:"
echo "- Initial balance: \$0.00"
echo "- After deposit: \$50.00"
echo "- After coffee: \$45.50"
echo "- After salary: \$3045.50"
echo "- Final balance: \$3000.01"
echo "- Idempotency should prevent duplicate salary"
echo "- Invalid transactions should return error responses"