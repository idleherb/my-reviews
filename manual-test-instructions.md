# Manual Testing Instructions

Since there are issues with the node processes, here's how to test manually:

## 1. Kill existing Node processes

In Activity Monitor:
- Search for "node" 
- Select all node processes
- Click the (X) button to force quit them

## 2. Start the server manually

Open a new Terminal window and run:
```bash
cd /Users/eric.hildebrand/dev/public/idleherb/my-reviews/server
node index.js
```

You should see:
```
Server is running on port 3000
Environment: development
```

## 3. Test the server

In another Terminal window:
```bash
curl http://localhost:3000/api/health
```

Should return:
```json
{"status":"healthy","timestamp":"...","message":"Server is running"}
```

## 4. Android Emulator Configuration

The app is already configured to use `10.0.2.2:3000` which is the correct address for the Android emulator to reach your Mac's localhost.

## 5. Test in Android App

1. Open the app in the Android emulator
2. Go to Settings (3-dots menu)
3. The server address should already be set to `10.0.2.2`
4. Enable "Mit Server synchronisieren"
5. Click "Verbindung testen"

## Troubleshooting

If "Verbindung testen" fails:
- Make sure the server is running (check Terminal)
- Check that port is 3000
- Try using your Mac's actual IP address instead of 10.0.2.2:
  - Run `ifconfig | grep "inet " | grep -v 127.0.0.1`
  - Use the IP that starts with 192.168.x.x or 10.x.x.x

## Alternative: Use without PostgreSQL

The current setup uses a simplified health endpoint that doesn't require PostgreSQL. To test the full functionality with database:

1. Install PostgreSQL: `brew install postgresql`
2. Start it: `brew services start postgresql`
3. Run the setup script: `./setup-local-db.sh`
4. Update `server/index.js` to use `./routes/health` instead of `./routes/health-simple`