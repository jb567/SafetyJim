import * as Discord from 'discord.js';
import * as log from 'winston';
import * as fs from 'fs';

const client = new Discord.Client();
const config = JSON.parse(fs.readFileSync('./config.json').toString()) as IConfig;

interface IConfig {
    token: string;
}

client.on('ready', () => {
    log.info(`Client is ready, username: ${client.user.username}.`);
});

client.on('message', (msg) => {
    if (msg.channel.type === 'dm') {
        return;
    }
    if (msg.content === 'ping') {
        msg.reply('pong');
    }
});

client.login(config.token);