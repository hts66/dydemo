$filePath = "c:\Users\27036\Desktop\dydemo\dyqianduan\src\views\My.vue"
$content = Get-Content $filePath -Raw

$cssToAdd = @'

.background-hint {
  position: absolute;
  bottom: 20px;
  right: 60px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 16px;
  color: white;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.3s;
  cursor: pointer;
}

.profile-header-section:hover .background-hint {
  opacity: 1;
}

.background-modal {
  position: fixed;
  top: 0;
  left: 200px;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
}

.background-modal-content {
  background: #2a2a2a;
  border-radius: 12px;
  width: 400px;
  max-width: 90vw;
  overflow: hidden;
}

.background-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  border-bottom: 1px solid #3a3a3a;
}

.background-modal-header h3 {
  color: #fff;
  font-size: 18px;
  margin: 0;
}

.background-colors {
  display: flex;
  flex-wrap: wrap;
  padding: 20px;
  gap: 16px;
  justify-content: center;
}

.color-item {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border: 2px solid transparent;
}

.color-item:hover {
  transform: scale(1.1);
}

.color-item.active {
  border-color: #fff;
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.1);
}

'@

$content = $content -replace '\.hidden-input \{', $cssToAdd + ".hidden-input {"

Set-Content -Path $filePath -Value $content -Encoding UTF8
Write-Host "CSS added successfully"
