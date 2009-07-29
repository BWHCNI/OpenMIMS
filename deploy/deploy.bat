xcopy ..\build\classes . /S /Y
jar -cvfM Open_MIMS.jar com org *.class plugins.config
xcopy Open_MIMS.jar "\Program Files\ImageJ\plugins" /Y
