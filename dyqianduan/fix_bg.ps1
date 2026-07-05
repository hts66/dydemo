$filePath = "c:\Users\27036\Desktop\dydemo\dyqianduan\src\views\Profile.vue"
$content = Get-Content $filePath -Raw

$content = $content -replace '\.card-content \{[\s\S]*?\}', '.card-content {
  padding: 40px;
  padding-top: 0;
  pointer-events: none;
}

.card-content > * {
  pointer-events: auto;
}'

Set-Content -Path $filePath -Value $content -Encoding UTF8
Write-Host "Fixed card-content style"
