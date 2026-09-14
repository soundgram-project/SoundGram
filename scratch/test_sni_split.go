package main

import (
	"crypto/tls"
	"fmt"
	"net"
	"os"
)

func findSNISplitPos(p []byte) int {
	if len(p) < 44 || p[0] != 0x16 || p[5] != 0x01 {
		return -1
	}

	sessionIDLen := int(p[43])
	offset := 44 + sessionIDLen
	if len(p) < offset+2 {
		return -1
	}

	cipherLen := int(p[offset])<<8 | int(p[offset+1])
	offset += 2 + cipherLen
	if len(p) < offset+1 {
		return -1
	}

	compLen := int(p[offset])
	offset += 1 + compLen
	if len(p) < offset+2 {
		return -1
	}

	extTotalLen := int(p[offset])<<8 | int(p[offset+1])
	offset += 2
	extEnd := offset + extTotalLen
	if extEnd > len(p) {
		extEnd = len(p)
	}

	for offset+4 <= extEnd {
		extType := int(p[offset])<<8 | int(p[offset+1])
		extLen := int(p[offset+2])<<8 | int(p[offset+3])
		offset += 4
		if offset+extLen > extEnd {
			break
		}

		if extType == 0x0000 && extLen >= 5 { // server_name
			nameType := p[offset+2]
			nameLen := int(p[offset+3])<<8 | int(p[offset+4])
			nameOffset := offset + 5
			if nameType == 0x00 && nameOffset+nameLen <= offset+extLen {
				sni := string(p[nameOffset : nameOffset+nameLen])
				fmt.Printf("Found SNI: %s (offset: %d, len: %d)\n", sni, nameOffset, nameLen)
				return nameOffset + 3
			}
		}
		offset += extLen
	}
	return -1
}

func main() {
	l, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		panic(err)
	}
	defer l.Close()

	go func() {
		conf := &tls.Config{
			ServerName: "music.youtube.com",
			InsecureSkipVerify: true,
		}
		conn, err := tls.Dial("tcp", l.Addr().String(), conf)
		if err == nil {
			conn.Close()
		}
	}()

	srvConn, err := l.Accept()
	if err != nil {
		panic(err)
	}
	defer srvConn.Close()

	buf := make([]byte, 2048)
	n, err := srvConn.Read(buf)
	if err != nil {
		panic(err)
	}
	hello := buf[:n]
	fmt.Printf("Read ClientHello length: %d bytes\n", len(hello))

	splitPos := findSNISplitPos(hello)
	fmt.Printf("Split pos: %d\n", splitPos)
	if splitPos > 0 {
		part1 := hello[:splitPos]
		part2 := hello[splitPos:]
		fmt.Printf("Part1 len: %d, Part2 len: %d\n", len(part1), len(part2))
	}
	os.Exit(0)
}
