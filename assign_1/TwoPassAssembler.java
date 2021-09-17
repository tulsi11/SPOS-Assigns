import java.io.BufferedReader;
import java.util.HashMap;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.FileNotFoundException;

class Symbol {
    String symbolName;
    int address;
    int length;

        Symbol(String symbolName) {
            this.symbolName =  symbolName;
        }

        Symbol(String symbolName, int addr) {
            this.symbolName = symbolName;
            this.address = addr;
        }

        void setLength(int len) {
            this.length = len;
        }
        public static void main(String args[]){
            System.out.println(".");
        }

    
}

class Literal {
    String litName;
    int address;
}

class Assembler {
    HashMap<String, Integer> MOT = new HashMap<String, Integer>();   //Mnemonic OpCode Table
    HashMap<String, Integer> POT = new HashMap<String, Integer>();   // Pseudo OpCode Table
    HashMap<String, Integer> RT = new HashMap<String, Integer>();    //Register Table
    HashMap<String, Integer> BCCODE = new HashMap<String, Integer>();  //Condition Code Table

    int poolTab[] = new int[10];     //POOL TABLE
    Symbol symTab[] = new Symbol[20]; //SYMBOL TABLE
    Literal litTab[] = new Literal[20]; //LITERAL TABLE

    int sindex, l_index, pindex, loc;

    Assembler() {

        sindex = 0;
        l_index = 0;
        pindex = 0;
        loc = 0;
        poolTab[0] = 0;

         // Adding Imperative Statements to Mnemonic Op Code Table
         MOT.put("STOP", 0);
         MOT.put("MOVER", 1);
         MOT.put("MOVEM", 2);
         MOT.put("ADD", 3);
         MOT.put("SUB", 4);
         MOT.put("MUL", 5);
         MOT.put("DIV", 6);
         MOT.put("BC", 7);

         // Adding Assembler Directives to Psuedo OpCode Table
        POT.put("START", 1);
        POT.put("END", 2);
        POT.put("LTORG", 3);
        POT.put("DS", 4);
        POT.put("DC", 5);

        // Registers Table
        RT.put("AREG", 1);
        RT.put("BREG", 2);
        RT.put("CREG", 3);
        RT.put("DREG", 4);

        //Branch on condition codes
        BCCODE.put("LT", 1);
        BCCODE.put("LE", 1);
        BCCODE.put("GT", 1);
        BCCODE.put("GE", 1);
        BCCODE.put("EQ", 1);
        BCCODE.put("ANY", 1);
    }

        //Method to search whether the symbol is present in the Symbol Table
        int search(String s) {
            for(int i = 0; i < sindex; i++) {
                if(symTab[i].symbolName.equals(s)){
                    return i;
                }
            }
            return -1;
        }

        void printTables() throws Exception {
            FileWriter fw = new FileWriter("C:\\Users\\Tulsi Chopade\\Desktop\\SPOS\symboltable.txt");
            fw.write("Sym_Name\tAddr\tLength\n");
            for(int i = 0; i < sindex; i++) {
                fw.write(symTab[i].symbolName + "\t" + symTab[i].address + "\t" + symTab[i].length + "\n");
            }
            fw.close();  //Close Symbol Table file

            fw = new FileWriter("C:\\Users\\Tulsi Chopade\\Desktop\\SPOS\\literal_table.txt");
            fw.write("lName\tAddress\n");
            for(int i = 0; i < l_index; i++) {
                fw.write(litTab[i].litName + "\t" + litTab[i].address + "\n");
            }
            fw.close();   //Close Literal Table file

            fw = new FileWriter("C:\\Users\\Tulsi Chopade\\Desktop\\SPOS\\pooltable.txt");
            fw.write("Pool_Index\n");
            for(int i = 0; i < pindex; i++) {
                fw.write(poolTab[i] + "\n");
            }
            fw.close(); //CLose pool table file
        }


        void passOne(BufferedReader br) throws Exception {
            String stmt = "";
            int k;

            FileWriter fw = new FileWriter("C:\\Users\\Tulsi Chopade\\Desktop\\SPOS\\pass1output.txt");
            while((stmt = br.readLine()) != null) {
                String token[] = stmt.split("[ \t]+");  //Split the statement using spaces
                
                if(token.length > 4) {
                    System.out.println("Invalid Statement");
                }
                else {
                    if(token.length == 4) {
                        k = search(token[0]);
                        if(k == -1) {
                            //if token is not present then add the symbol and Location counter to the symbol table
                            symTab[sindex] = new Symbol(token[0], loc); 
                            
                            //inc symbol table pointer
                            sindex++;
                        }
                        else {
                            //Symbol is already present
                            System.out.println("Duplicate label");
                        }

                        //Shift the tokens
                        token[0] = token[1];
                        token[1] = token[2];
                        token[2] = token[3];
                    }
                    
                    
                    if(POT.containsKey(token[0])) { //check in pseudo op code table
                        int val = POT.get(token[0]);  //Get corresponding Machine code of pseudo op code
                        if(token[0].equals("START")) {
                            if(token.length == 2) {  
                                loc = Integer.parseInt(token[1]);   //Get the Location Counter
                            }
                            else if(token.length > 2) {
                                System.out.println("Invalid Instruction");
                                fw.write("Error");
                            }
                            System.out.println("AD " + val + " - C " + loc);
                            fw.write("AD " + val + " - C " + loc + "\n");
                        }

                        //If end statement is encountered 
                        else if(token[0].equals("END")) {
                            //Process all the literals and allocate address to them
                            for(int i = poolTab[pindex]; i < l_index; i++) {
                                litTab[i].address = loc;  //Allocate address to the literal = Location counter
                                loc++;  
                            }
                            System.out.println("AD " + val);
                            fw.write("AD " + val + "\n");   //write to the file
                        }

                        else if(token[0].equals("LTORG")) {
                            for(int i = poolTab[pindex]; i < l_index; i++) {
                                litTab[i].address = loc;
                                loc++;
                            }
                            System.out.println("AD " + val);
                            fw.write("AD " + val + "\n");

                            // after allocating address to literals, increase pool table index and store literal table's next starting index in pool table
                            pindex++;
                            poolTab[pindex] = l_index;
                        }
                    }

                    //Check if token is present in Mnemonic Op Code table
                    else if(MOT.containsKey(token[0])) {
                        int val = MOT.get(token[0]);
                        int reg = 0;
                        int cc_val = 0;
                        int flag = 0;

                        char ch;

                        if(RT.containsKey(token[1])) {
                            reg = RT.get(token[1]);
                        }
                        if(BCCODE.containsKey(token[1])) {
                            flag = 1; //found
                            cc_val = BCCODE.get(token[1]);
                        }

                        if(token[2].charAt(0) == '='){
                            // if token 2 starts with = sign then it is a literal
                            ch = 'L';
                            litTab[l_index] = new Literal();
                            litTab[l_index].litName = token[2].substring(1); //Store the literal name in literal table
                            k = l_index; 
                            l_index++;  //increment l_index to point to next empty location
                        }
                        else {
                            ch = 'S'; //character is symbol
                            k = search(token[2]);  
                            if(k == -1) {
                                symTab[sindex] = new Symbol(token[2]);
                                k = sindex;
                                sindex++;
                            }
                        }

                        if(flag == 1) {
                            System.out.println("IS " + val + " " + cc_val + " " + ch + " " + k);
                            fw.write("IS " + val + " " + cc_val + " " + ch + " " + k + "\n");
                        }
                        else {
                            System.out.println("IS " + val + " " + reg + " " + ch + " " + k);
                            fw.write("IS " + val + " " + reg + " " + ch + " " + k + "\n");
                        }
                        loc++;
                    } 
                    else{
                        if(token[1].equals("DS") || token[1].equals("DC")){
                            int len = Integer.parseInt(token[2]); ///Get length of declaration stmt
                            k = search(token[0]);
    
                            if(k == -1){
                                symTab[sindex] = new Symbol(token[2]);
                                k = sindex;
                                sindex++;
                            }
                            if(token[1].equals("DS")){
                                int val = POT.get(token[1]);
                                System.out.println("DL " + val + " - C " + token[2]);
                                fw.write("DL " + val + " - C " + token[2] + "\n");
                               symTab[k].setLength(len);
                               symTab[k].address = loc;
                                loc += len;
                            }
                            else{
                                int val = POT.get(token[1]);
                                System.out.println("DL " + val + " - C " + token[2]);
                                fw.write("DL " + val + " - C " + token[2] + "\n");
                                symTab[k].setLength(1);
                                symTab[k].address = loc;
                                loc += 1;
                            }
                        }
                    }
                }
            }
            fw.close();
            printTables();

            System.out.println("-------------Symbol Table-------------");
            System.out.println("sindex\tsname\taddr\tlen\n");
            for(int i = 0; i < sindex; i++){
                System.out.println(i+ "\t"  + symTab[i].symbolName + "\t" + symTab[i].address + "\t" + symTab[i].length + "\n");
            }

            System.out.println("-------------Literal Table-------------");
            System.out.println("lindex\tlname\taddress\n");
            for(int i = 0; i < l_index; i++){
                System.out.println(i + "\t" + litTab[i].litName + "\t" + litTab[i].address  + "\n");
            }

            System.out.println("-------------Pool Table-------------");
            System.out.println("PoolIndex\n");
            for(int i = 0; i <= pindex; i++){
                System.out.println(poolTab[i] + "\n");
            }

        }


        void passTwo() throws FileNotFoundException, Exception{
            String stmt = ""; // to read statement
            int t_index = 0;
            int loc = 0; //Location counter
            BufferedReader readFile = new BufferedReader(new FileReader("pass1output.txt"));

            FileWriter fw = new FileWriter("pass2output.txt");
            System.out.println("--------------------------- Machine Code --------------------------");

            while((stmt = readFile.readLine()) != null) {
                String token[] = stmt.split("[ ]+");
                if(token.length == 5) {
                    if(token[0].equals("AD")){
                        //START STATEMENT
                        loc = Integer.parseInt(token[4]);
                        // System.out.println("***************************" + loc);
                    }
                    else if(token[0].equals("IS")){
                        //for imperative statement
                        if(token[3] == "L")  {
                            //Literal
                            System.out.println(loc + ") " + token[1] + " " + token[2] + " " + litTab[Integer.parseInt(token[4])].address);
                            fw.write(loc + ") " + token[1] + " " + token[2] + " " + litTab[Integer.parseInt(token[4])].address + "\n");
                        }
                        else {
                            //for symbol
                            System.out.println(loc + ") " + token[1] + " " + token[2] + " " + symTab[Integer.parseInt(token[4])].address);
                            fw.write(loc + ") " + token[1] + " " + token[2] + " " + symTab[Integer.parseInt(token[4])].address + "\n");
                        }
                        //After processing literal or symbol increment the LC
                        loc++;
                    }
                    else {
                        //Declarative Statement
                        if(Integer.parseInt(token[1]) == 5){
                            //DC stmt
                            System.out.println(loc + ") " + 0 + " " + 0 + " " + token[4]);
                            fw.write(loc + ") " + 0 + " " + 0 + " " + token[4] + "\n");
                            loc += 1;
                        }
                        else {
                            //DS STATEMENT
                            System.out.println(loc + ") " + 0 + " " + 0 + " " + token[4]);
                            fw.write(loc + ") " + 0 + " " + 0 + " " + token[4] + "\n");
                            loc = loc + Integer.parseInt(token[4]);
                        }
                    }
                }// end of if

                else {
                    if(token[1].equals("2")){
                        //END STATEMENT
                        loc += (l_index - poolTab[t_index]);
                    }
                    else {
                        //LTORG STATEMENT
                        loc = loc + (poolTab[t_index] + 1 - poolTab[t_index]);
                        t_index++;
                    }
                }
                
            }
            readFile.close();  //close pass1output file
            fw.close(); //close pass2output.file
        }

}

public class TwoPassAssembler {
    
    public static void main(String[] args) throws FileNotFoundException, Exception {
        File f = new File("C:\\Users\\Tulsi Chopade\\Desktop\\SPOS\\input.txt");
        BufferedReader br = new BufferedReader(new FileReader(f));

        Assembler asm = new Assembler();
        System.out.println("----------------------------------");
        System.out.println("Pass 1");
        System.out.println("----------------------------------");
        asm.passOne(br);
        System.out.println("----------------------------------");
        System.out.println("Pass 2");
        asm.passTwo();
        System.out.println("----------------------------------");
    }
}

