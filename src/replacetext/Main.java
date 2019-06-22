package replacetext;

/**
 * Program umożliwia seryjne wyszukanie i zastąpienie frazy-klucza w dowolnym dokumencie tekstowym (Dokument.doc)
 * danymi zawartymi w pliku "Dane.txt", żeby nie trzeba było ręcznie kopiować i wklejać wiele razy tekstu, który chcemy wpisać
 * do dokumentu. Wystarczy w pliku Dane.txt umieścić tekst, który chcemy wkleić oraz w dokumencie docelowym specjalną frazę
 * w miejscu, w którym będziemy wklejać tekst. Frazę tę umieszczamy również na początku pliku Dane.txt żeby program wiedział
 * czego szukać. Na końcu pliku dodajemy cokolwiek (np. koniec) w ostatniej linii już pod naszymi danymi żeby nie natknąć się
 * na niespodziewany koniec pliku (EOF). Przykładowo mamy zaproszenie, do którego chcemy dodać imię i nazwisko. Problem w tym,
 * że tych zaproszeń potrzeba np. 120 i na każdym mają widnieć inne dane. Wpisujemy więc frazę-klucz na zaproszeniu i taką
 * stronę kopiujemy 119 razy (oczywiście po 10 stronach wklejamy już po 10 stron żeby było szybciej :-), po czym odpalamy program.
 * @author Dominik Marcinkowski
 * @version 1.1
 * (dlTekstuDoZamiany jest teraz odczytywana z pliku dane.txt.)
 * (Poprawiono również działanie w przypadku, gdy któraś z linii w Dane.txt osiągnie maksymalną długość, czyli długość hasła.)
 * (W poprzedniej wersji program wczytałby złe dane.)
 * @version 1.2
 * (Oprogramowano odczyt i zapis pliku docelowego tak, aby nie napotkać EOF, dzięki czemu nie wystąpi IO Exception.)
*/

import java.io.*;

public class Main
{
	private static int dlTekstuDoZamiany;										// w bajtach
	private static int[][] dane;
	private static int[] tekstDoZamiany;
	private static int ileDanych = 0;											// ile istotnych linii tekstu zawiera Dane.txt (taka będzie długość tablicy dane - 1. wymiaru)
	private static int elementDanych = 0;										// który wiersz z tablicy dane teraz wypiszemy
	
	public static void main(String[] args)
	{
		try
		{
			RandomAccessFile RAFdane = new RandomAccessFile("Dane.txt", "r");
			
			policzIleDanych(RAFdane);
			RAFdane.seek(0);
			odczytajDane(RAFdane);

			RAFdane.close();
		}
		catch (IOException e)
		{
			System.out.println(e.getMessage());
			System.out.println("RAFdane IO Exception");
		}
		
		try
		{
			RandomAccessFile RAFdok = new RandomAccessFile("Dokument.doc", "rw");

			znajdzIzamien(RAFdok);
			
			RAFdok.close();
		}
		catch (IOException e)
		{
			System.out.println(e.getMessage());
			System.out.println("RAFdok IO Exception");
		}

	}
	
	private static void policzIleDanych(RandomAccessFile plik) throws IOException
	{
		String tempS = plik.readLine();								// najpierw odczytujemy frazę-klucz żeby poznać jej długość
		dlTekstuDoZamiany = tempS.length() - 2;						// odejmujemy 2 puste bajty (10 0, które są na początku każdej linii, ale nie są częścią tekstu)
		
		while (tempS != null)
		{
			tempS = plik.readLine();								// odczytuje wszystkie pozostałe linie tekstu aż do końca pliku
			if (tempS != null)
				ileDanych += 1;
		}
		
		ileDanych = ileDanych / 2 - 1;								// ileDanych dzielone przez 2, gdyż policzyło też puste linie oraz odjąć 'koniec'
		dane = new int[ileDanych][dlTekstuDoZamiany];				// tworzy tablicę, w której zapiszemy wszystkie nasze źródłowe dane
	}
	
	private static void odczytajDane(RandomAccessFile plik) throws IOException
	{																// czytamy dane z pliku "Dane.txt" i umieszczamy w tablicy "dane"
		int byte1 = 0;												// na początek trzeba odczytać słowo-klucz do podmianki
		int byte2 = 0;
		String tempS = "";
		tekstDoZamiany = new int[dlTekstuDoZamiany];
		
		plik.readByte();
		plik.readByte();

		for (int i = 0; i < dlTekstuDoZamiany / 2; i++)				// wyjaśnienie poniżej
		{
			byte1 = plik.readByte();
			byte2 = plik.readByte();
			tempS = tempS + byte1 + byte2;
			tekstDoZamiany[i*2] = byte1;
			tekstDoZamiany[i*2+1] = byte2;
		}
		
		plik.readByte();											// pomijamy dwa bajty bez zapisywania bo to dodatkowa pusta linia w systemie Unicode
		plik.readByte();
		
		for (int j = 0; j < dane.length; j++)						// duża pętla for skacze po wierszach tablicy i zapisuje 'dlTekstuDoZamiany' bajtów w każdym wierszu
		{
			byte1 = 0;												// czytamy po dwa bajty, czyli cały znak w tekście
			byte2 = 0;
			tempS = "";												// jako, że wszystko zapisujemy w tablicach użyłem też Stringa dla ułatwienia sprawdzania co się zapisało
			
			plik.readByte();										// pomijamy dwa bajty bez zapisywania jak wyżej
			plik.readByte();
			
			for (int i = 0; i < dlTekstuDoZamiany / 2; i++)			// mała pętla for odczytuje całe linie tekstu z "Dane.txt" aż do znaku końca linii dodając na końcu znaki spacji
			{														// żeby zapełnić cały wiersz tablicy (jeśli źródłowy tekst był krótszy niż dlTekstuDoZamiany)
				if ( !(byte1 == 13 && byte2 == 0) )					// jeśli w poprzedniej iteracji nie zczytało końca linii to czytaj następne bajty
				{
					byte1 = plik.readByte();
					byte2 = plik.readByte();
				}
				if ( !(byte1 == 13 && byte2 == 0) )					// jeśli w tej iteracji również nie zczytało entera to dopisz bieżący znak do "tempS" oraz tablicy "dane"
				{
					tempS = tempS + byte1 + byte2;
					dane[j][i*2] = byte1;							// kolejne dane są zapisywane w elemencie tablicy o indeksie parzystym
					dane[j][i*2+1] = byte2;							// oraz nieparzystym
				}
				else												// jeśli mamy koniec linii to dopisz spacje do tablicy i w następnych iteracjach nie odczytuj
				{													// kolejnych danych dopóki nie zapełnisz bieżącego wiersza spacjami do końca
					dane[j][i*2] = 32;
					dane[j][i*2+1] = 0;
				}
			}
			
			if ( !(byte1 == 13 && byte2 == 0) )						// jeśli w małej pętli for nie zczytało końca linii to znaczy, że skończyła się pętla,
			{														// bo wiersz miał maksymalną długość. Trzeba jeszcze przesunąć się o enter.
				plik.readByte();
				plik.readByte();
			}
		}
	}
	
	private static void znajdzIzamien(RandomAccessFile plik) throws IOException
	{
		int tempByte = 0;
		int ileDopasowano = 0;
		
		while (plik.getFilePointer() < plik.length() )
		{

			for (int i = 0; i < tekstDoZamiany.length; )
			{
				tempByte = plik.readByte();
				if (tempByte == tekstDoZamiany[i])					// jeśli odczytany bajt odpowiada temu w tablicy z szukaną frazą
				{
					i++;											// zwiększamy "i" żeby potem porównywać z następnym bajtem z tablicy
					if (i > ileDopasowano)							// jako, że "i" będzie zerowane za każdym razem gdy sprawdzany bajt nie będzie odpowiadał
						ileDopasowano++;							// szukanemu, musimy mierzyć globalne "rekordy" ilości dopasowań jeśli tylko "i" będzie
				}													// większe od poprzednio rekordowej wartości
				else if (i > 0)										// jeśli np. "i" osiągnęło wartość 4 oznacza to, że wystąpiły 4 bajty po kolei, które pasowały
				{													// do czterech pierwszych bajtów w szukanej frazie
					plik.seek(plik.getFilePointer() - 1);			// ale nie do wszystkich, więc trzeba się cofnąć o jeden bajt, żeby móc go dalej porównywać
					i = 0;											// z pierwszym bajtem w tablicy a nie tylko z którymś z kolei, na który wskazywało "i"
				}
				if (tempByte == 13)									// tutaj wybiegamy w przyszłość i jeśli bieżący bajt zwiastuje, że następny może być końcem linii
				{													// (13 to pół entera a 0 to drugie pół)
					tempByte = plik.readByte();						// czytamy następny bajt żeby się o tym przekonać
					if (tempByte == 0)
						break;										// i jeśli tak jest przerywamy pętlę for
					else											// a jeśli ta "13" nie była od entera (bo po niej nie ma "0") to cofamy sprawdzenie "0"
						plik.seek(plik.getFilePointer() - 1);		// i normalnie porównujemy dalej z tablicą "tekstDoZamiany"
				}													// są trzy możliwości wyjścia z pętli for: 1. wtedy, gdy znajdziemy szukaną frazę, czyli "i" osiągnie wartość 'dlTekstuDoZamiany',
																	// 2. gdy natkniemy się na koniec linii i pętla zostanie przerwana instrukcją break
				if (plik.getFilePointer() == plik.length())			// lub 3. gdy jest to ostatnia linia pliku, która nie kończy się enterem lecz EOF. Sprawdzamy więc czy dotarliśmy do ostatniego bajtu.
					break;
			}
																	// teraz trzeba sprawdzić przyczynę zakończenia pętli for
			if (ileDopasowano == tekstDoZamiany.length)				// jeżeli "ileDopasowano" wynosi tyle samo co dlTekstuDoZamiany oznacza to, że odszukaliśmy nasze miejsce, gdzie chcemy wpisać nasze dane
			{														// cofamy się o 'dlTekstuDoZamiany' bajtów żeby podmienić tekst na nasz z tablicy "dane"
				plik.seek(plik.getFilePointer() - tekstDoZamiany.length);
				if (elementDanych < dane.length)					// zliczamy który to już raz znaleźliśmy słowo-klucz żeby wiedzieć, który element
					elementDanych += 1;								// z tablicy "dane" mamy teraz zapisać w dokumencie
				zapiszDanedoDokumentu(plik);
				ileDopasowano = 0;									// po zapisaniu zerujemy zmienną żeby dalej szukać w dokumencie miejsc do zastąpienia
			}														// jeżeli "ileDopasowano" nie osiągnęło dlTekstuDoZamiany to znaczy, że osiągnęliśmy koniec linii i nie znaleźliśmy naszej frazy

		}
	}
	
	private static void zapiszDanedoDokumentu(RandomAccessFile plik) throws IOException
	{																// w zależności od tego, który już raz znaleźliśmy szukaną frazę odczytujemy właściwy indeks tablicy "dane"
		for (int i = 0; i < dane[elementDanych - 1].length; i++)	// (kolejny) i każdy bajt z tego wiersza zapisujemy do pliku "Dokument.doc", czyli nasze pełne 'dlTekstuDoZamiany' bajtów wraz ze spacjami
		{
			plik.writeByte(dane[elementDanych - 1][i]);
		}
	}
}






