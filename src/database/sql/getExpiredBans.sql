SELECT * FROM BanList WHERE ExpireTime < (strftime('%s','now')) and Expires = 1;