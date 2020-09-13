# STFU

We put the "poof" in "spoofed calls".

STFU is a phone call blocker for Android Q and above.

Things STFU does *NOT* do:
- Caller ID
- Cloud sync
- Steal your contact list
- Make girls call you back

Things STFU does do:
- Block phone numbers by prefix

You can think of this as the phone call equivalent of
[NekoSMS](https://github.com/apsun/NekoSMS), although this app is not
nearly as polished.

If you live in the US, this app also comes with a database of all the area
codes by state. You can use this to easily block all calls coming from an
entire state (which is super useful if your number is from that state, but
you no longer live there and keep getting spam calls with spoofed numbers).

## Requirements

- Android phone running Android Q (10) or newer

## Notes

This app cannot block callers on your contact list. This is a limitation
of the API we use,
[CallScreeningService](https://developer.android.com/reference/android/telecom/CallScreeningService).
An interesting consequence of this is that you can block all non-contacts
by making a filter rule that matches everything (i.e. `+`).

## License

MIT
