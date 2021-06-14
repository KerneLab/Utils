java -cp appXlsxImp.jar org.kernelab.utils.sql.ExcelImporter -file E:\project\Utils\dat\test.xlsx -sheet 0 -table jdl_test_import -action insert -url jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=UTF-8 -usr test -pwd test
pause
