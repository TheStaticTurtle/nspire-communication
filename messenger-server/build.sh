pip2 install -r requirement.txt
python2 -m PyInstaller --onefile server.py
cd dist/
chmod 777 ./server
mv server "server-messenger_"$(uname -m)
cd ..
rm -rf build
rm *.spec
