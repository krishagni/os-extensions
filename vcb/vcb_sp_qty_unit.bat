@ECHO off
SET app_url=""
SET userName=""
SET password=""

IF "%1" NEQ "" (
  GOTO = %1
    :-url 
      SHIFT
      SET app_url=%1
	  SHIFT
	  IF NOT "%1" == "" ( GOTO = %1 )
    
	:-user
      SHIFT
      SET userName=%1
	  SHIFT
	  IF NOT "%1" == "" ( GOTO = %1 )
	  
	:-pass
      SHIFT
      SET password=%1
	  SHIFT
	  IF NOT "%1" == "" ( GOTO = %1 )
	  
) ELSE ( ECHO "please enter paramerters")

IF "%app_url%" EQU "" ( 
  ECHO "Application URL  not specified. Use -url"
  EXIT /b
)

IF "%userName%" EQU "" ( 
  ECHO "User name not specified. Use -user"
  EXIT /b
)

IF "%password%" EQU "" ( 
  ECHO "Password not specified. Use -pass"
  EXIT /b
)
	
curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Cell\",\"type\":\"MNC Bone Marrow\",\"unit\":\"cell count\",\"htmlDisplayCode\":\"\"}"

curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Cell\",\"type\":\"MNC Bone Marrow with Trizol\",\"unit\":\"cell count\",\"htmlDisplayCode\":\"\"}"

curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Cell\",\"type\":\"PBMNC\",\"unit\":\"cell count\",\"htmlDisplayCode\":\"\"}"

curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Molecular\",\"type\":\"DNA\",\"unit\":\"ug\",\"htmlDisplayCode\":\"\"}"

curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Molecular\",\"type\":\"ctDNA\",\"unit\":\"ug\",\"htmlDisplayCode\":\"\"}"

curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Molecular\",\"type\":\"RNA\",\"unit\":\"ug\",\"htmlDisplayCode\":\"\"}"

curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Tissue\",\"type\":\"Departmental FFPE Block\",\"unit\":\"count\",\"htmlDisplayCode\":\"\"}"

curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Tissue\",\"type\":\"EBUS\",\"unit\":\"count\",\"htmlDisplayCode\":\"\"}"

curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Tissue\",\"type\":\"Ethanol Fixed Paraffin H"&"E Slide\",\"unit\":\"count\",\"htmlDisplayCode\":\"\"}"

curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Tissue\",\"type\":\"Ethanol Fixed Paraffin Slide\",\"unit\":\"count\",\"htmlDisplayCode\":\"\"}"

curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Tissue\",\"type\":\"ETOH-Fixed Paraffin Block\",\"unit\":\"count\",\"htmlDisplayCode\":\"\"}"

curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Tissue\",\"type\":\"FFPE Block\",\"unit\":\"count\",\"htmlDisplayCode\":\"\"}"

curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Tissue\",\"type\":\"FFPE H"&"E Slide\",\"unit\":\"count\",\"htmlDisplayCode\":\"\"}"

curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Tissue\",\"type\":\"FFPE Slide\",\"unit\":\"count\",\"htmlDisplayCode\":\"\"}"

curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Tissue\",\"type\":\"Fresh Tissue\",\"unit\":\"count\",\"htmlDisplayCode\":\"\"}"

curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Tissue\",\"type\":\"Microdissected\",\"unit\":\"count\",\"htmlDisplayCode\":\"\"}"

curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Tissue\",\"type\":\"Not Specified\",\"unit\":\"count\",\"htmlDisplayCode\":\"\"}"

curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Tissue\",\"type\":\"OCT Block\",\"unit\":\"count\",\"htmlDisplayCode\":\"\"}"

curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Tissue\",\"type\":\"OCT Slide\",\"unit\":\"count\",\"htmlDisplayCode\":\"\"}"

curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Tissue\",\"type\":\"Snap Frozen\",\"unit\":\"count\",\"htmlDisplayCode\":\"\"}"

curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Tissue\",\"type\":\"Tissue Micro Array\",\"unit\":\"count\",\"htmlDisplayCode\":\"\"}"

curl -u %userName%:%password% -X POST -H "Accept: Application/json" -H "Content-Type: application/json" %app_url%/rest/ng/specimen-quantity-units -d "{\"specimenClass\":\"Tissue\",\"type\":\"FFPE Core\",\"unit\":\"count\",\"htmlDisplayCode\":\"\"}"

EXIT /b