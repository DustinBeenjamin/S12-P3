/*
 * AUTHOR:      Benjamin Dustin
 * COURSE:      Computer Architecture
 * ASSIGNMENT:  PROJECT 2 - PIPELINES S12 SIMULATION
 * DESCRIPTION: ->
 *      This is the driver class of the entire project 2 simulator. To imitate a proccesor's sequential nature, the simulation
 *      uses an instance of the Hardware class to track the state of the proccesor between each clock. A hardware object has a clock method,
 *      which causes it to update its combinational logic. However, the order that the logic updates is too hard to manage.
 *      Instead, this program creates an initial Hardware object. For each clock cycle, it makes a copy of the object and calls
 *      the clock method on that object. The methods inside that object update the Hardware's value based on a reference to the 
 *      original Hardware object, not itself. Therefore, this program keeps track of a previous and current Hardware object.
 *      When the current object has been clocked, it then becomes the previous.
 * 
 *      This class also contains methods for processing and reporting metrics about the memfile that needs to be run on the processor.
 *
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;


public class Simulator {
    
    private final int MEMORY_BITS = 8;
    private final int MEMORY_LOCATIONS = (1 << MEMORY_BITS);

    //The simulation supports memfiles that do not start with non zero pc's and accumulators
    private int init_pc;
    private int init_accum;
    private Scanner scan;
    private Hardware prevHardware;
    private Hardware currentHardware;
    private boolean  simulation_done;

    //Reported Metrics (Calculated after simulation)
    private ArrayList<Stage5State> instruction_trace;
    private int num_clocks;
    private int num_stalls;
    private int alu_ops;                //AND, OR, ADD, SUB
    private int load_ops;               //LOAD, STORE
    private int store_ops;              //LOADI, STORE
    private int branch_ops;             //JUMP, JN, JZ
    private int misc_ops;               //HALT

    public static void main(String[] args) {
        String[] a = {"./Benchmarks/multiply/speed_multiply.mem"};
        // String[] a = args;

        //Instatiate a simulation object. The rest of the application runs from its constructor
        Simulator sim = new Simulator(a[0]);
    }

    /**
     * Runs the simulator with the provided memory file.
     *
     * @param args args[0] should be the filepath of the memfile that needs to be simulated
     */
    public Simulator(String filename) {
        //Initialize sim properties, inclusing initial memory
        simulation_done = false;
        int[] start_memory = instantiateMem(filename);

        //Use memfile info to create initial Hardware object
        prevHardware = new Hardware(init_pc, init_accum, start_memory, this);
        prevHardware.combinationalLogic(prevHardware);
        prevHardware.generateControlSignals(prevHardware);

        //Hardware will set simulation done when it finds a halt, call clock until that happens
        while (!simulation_done) { 
            currentHardware = prevHardware.clone();
            currentHardware.clock(prevHardware);
            prevHardware = currentHardware;
        }

        //Generate performace metrics and print result
        proccessResults(currentHardware.stage5States);
        System.out.println(this);
        estimateStallSource(currentHardware.stage5States);
    }

    //A hardware object will call this method when the program should terminate
    public void endSimulation(){
        System.out.println("Simulation Ended");
        simulation_done = true;
    }

    /**
     * At the end of each clock, the hardware object will take a snapshot of the last stage and add it to an array list.
     * All of the instructions that reached the last stage were executed. However, an instruction may sit idly 
     * in the last stage for 4 cycle if the pipline stalls. This information needs to be removed to produce the actual instruction 
     * trace.
     * @param s An array list of objects that hold information about the last stage of the pipeline
     */
    public void proccessResults(ArrayList<Stage5State> s) {
        num_clocks = s.size();
        instruction_trace = new ArrayList<>();
        num_stalls = 0;
       
        int clocks_to_fill_pipe = 4;
        int startup_clocks_lost = clocks_to_fill_pipe;
        int stalls_per_flush = clocks_to_fill_pipe;

        //Need to iterate through the instructions of 's', removing a set of 4 similar instrucions from each set of consequtive repeating instructions.
        //Instead of removing, just add the instructions that dont need removing to a new list 'instruction_trace'
        int repeat_state_counter = 0;
        
        
        //This loop doesnt start at 0 because the first four instructions are garbage. When the program starts,
        //the final register has 0's. This is intrepted as a jump instruction, but is not executed because it lacks
        //the control signals. To ignore this, save the previous element as the fifth element and start a loop from the sixth.
        Stage5State prev_state = s.get(startup_clocks_lost);
        Stage5State curr_state;
        addToInstructionTrace(prev_state);

        for (int i = clocks_to_fill_pipe + 1; i < s.size(); i++) {
            
            curr_state = s.get(i);

            //if the new state doesnt math the old
            if (!curr_state.equals(prev_state)) {
                //but the most recent instruction was a duplicate
                if (repeat_state_counter < stalls_per_flush) {
                    //add those duplicates to the instructoin trace if there was not 4 of them
                    for (int j = 0; j < repeat_state_counter; j++) {
                        addToInstructionTrace(prev_state);
                    }
                } 
                //either way, add the new instruction.
                addToInstructionTrace(curr_state);
                repeat_state_counter = 0;
            } else if (repeat_state_counter == stalls_per_flush) {
                //if the loop is iterating over repeated elements, only omit every 4 repeats
                addToInstructionTrace(curr_state);
                repeat_state_counter = 0;
            } else {
                //omit repeats if you havent found 4 of them yet.
                repeat_state_counter++;
            }

            //update the state
            prev_state = curr_state;
        }

        //each of skipped elements actually represented a stall, so do this to calculate how many stalls happened
        num_stalls = (s.size() - stalls_per_flush) - instruction_trace.size();
    }

    /**
     * Adds an elements to the intruction trace, and updates the running total of each type of instruction for the instruction mix.
     * @param s Element that needs to be added to instruction trace
     */
    public void addToInstructionTrace(Stage5State s) {
        instruction_trace.add(s);

        switch (s.opcode) {
            case Opcodes.ADD:
            case Opcodes.SUB:
            case Opcodes.AND:
            case Opcodes.OR :
                alu_ops++;
                break;
            case Opcodes.LOAD :
            case Opcodes.LOADI:
                load_ops++;
                break;
            case Opcodes.STORE:
            case Opcodes.STOREI:
                store_ops++;
                break;
            case Opcodes.JMP:
            case Opcodes.JN : 
            case Opcodes.JZ :
                branch_ops++;
                break;
            case Opcodes.HALT:
                misc_ops++;
                break;
            default:
                System.out.println("Cannot get accurate instructoin mix because opcode is not recognized.");
        }

    }

    /**
     * Translates a memfile into an array of integers that represent memory. Handles the following errors by quitting program
     * ->Non binary data
     * ->Non 8 bit initial pc
     * ->Non 12 bit inital accumulator
     * ->Non 12 bit instruction
     * ->Invalid opcode
     * ->etc
     * 
     * @param fileName the path where the memfile can be found. Should be passed int as args[0]
     */
    public int[] instantiateMem(String fileName) {
         int[] temp_mem = new int[MEMORY_LOCATIONS];
         
         
         try {
             
            if (!fileName.contains(".mem")) {
                throw new Exception("FUCK. The memfile path did not end in '.mem'");
            }
         
            scan = new Scanner(new File(fileName));
            String initPcString = scan.next();
            String initAccumString = scan.next();

            //Make sure the initial program counter is valid
            if (initPcString.length() != 8) {
                throw new Exception("Fuck. Initial PC value in memfile is not 8 characters");
            }

            //Make sure the initial accumulator value is 12 bits
            if (initAccumString.length() != 12) {
                throw new Exception("Fuck. Initial accumulator value in memfile is not 12 characters");
            }

            //Make sure memfile pc value is a binary string
            if (!validBinaryString(initPcString)) {
                throw new Exception("Fuck. Initial PC vlaue in memfile is not a binary string");
            }

            //Make sure memfile accumulator value is a binary string
            if (!validBinaryString(initAccumString)) {
                throw new Exception("Fuck. Initial accumulator value in memfile is not a binary string");
            }

            this.init_pc = (Integer.parseInt(initPcString, 2) & 0xFF);
            this.init_accum = (Integer.parseInt(initAccumString, 2) & 0xFFF);

            int i = 0;
            String instructionAddress;
            while (scan.hasNext()) {

                instructionAddress = scan.next();
                
                //Make sure the memfile doesnt provide more instructions than memory can handle
                if (i > (MEMORY_LOCATIONS - 1)) {
                    throw new Exception(String.format("Fuck. The memfile has more instructions that memeory allows. Line: %s ", instructionAddress));
                }

                //Make sure that the next line also has instruction data
                if (!scan.hasNext()) {
                    throw new Exception(String.format("Fuck. Instruction address %s has no data", instructionAddress));
                }

                //Grab the instruction binary string
                String newline = scan.next();

                //Make sure it is a valid binary string
                if (!validBinaryString(newline)) {
                    throw new Exception(String.format("Fuck. Memfile has invalid binary string. Line: %s", instructionAddress));
                }

                //Make sure its exactly 12 characters
                if (newline.length() != 12) {
                    throw new Exception(String.format("Fuck. Memfile string has %d characters. Exactly 12 is required. Line: %s", newline.length(), instructionAddress));
                }

                // Make sure it's a valid opcode
                int opcodeBinary = (Integer.parseInt(newline, 2)) >> 8;
                if (!Opcodes.isValidOpcode(opcodeBinary)) {
                    throw new Exception(String.format(
                        "Fuck. Memfile has illegal opcode %s on line %s",
                        String.format("%4s", Integer.toBinaryString(opcodeBinary & 0xF)).replace(' ', '0'),
                        instructionAddress
                    ));
                }

                //Save results in temp location
                temp_mem[i] = (Integer.parseInt(newline, 2) & 0xFFF);
                i++;
            }
        } catch (FileNotFoundException e) {
            System.out.println("Fuck. File not found.");
            if (scan != null) {
                scan.close();
            }
            System.exit(1);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            if (scan != null) {
                scan.close();
            }
            System.exit(1);
        }

        return temp_mem;

    }

    public void estimateStallSource(ArrayList<Stage5State> stage5States) {
        Stage5State prev_state = stage5States.get(4);
        Stage5State cur_state;
        ArrayList<Stage5State> repeats = new ArrayList<>();
        
        int repeat_counter = 0;

        for (int i = 0; i < stage5States.size(); i++) {
            cur_state = stage5States.get(i);
            repeat_counter = (cur_state.opcode != prev_state.opcode) ? 0 : (repeat_counter + 1);
            if (repeat_counter == 4) {
                repeats.add(cur_state);
                repeat_counter = 0;
            } 
            
            prev_state = cur_state;
        }

        int j_stalls = 0;
        int jz_stalls = 0;
        int jn_stalls = 0;
        int store_stalls = 0;
        int storei_stalls = 0;
        int other_stalls = 0;

        for (Stage5State s : repeats) {
            switch (s.opcode) {
                case Opcodes.JMP:
                    j_stalls++;
                    break;
                case Opcodes.JN:
                    jn_stalls++;
                    break;
                case Opcodes.JZ:
                    jz_stalls++;
                    break;
                case Opcodes.STORE:
                    store_stalls++;
                    break;
                case Opcodes.STOREI:
                    storei_stalls++;
                    break;
                default:
                    other_stalls++;
                    break;
            }
        }

        System.out.println("\nSTALL SOURCES");
        System.out.println("____________________");
        System.out.println("\tJMP:      " + j_stalls);
        System.out.println("\tJN:       " + jn_stalls);
        System.out.println("\tJZ:       " + jz_stalls);
        System.out.println("\tSTORE:    " + store_stalls);
        System.out.println("\tSTOREI:   " + storei_stalls);



    }

    /**
     * Determine if a string is binary
     * @param s string that needs to be checked
     * @return true if the string is binary, false otherwise
     */
    public boolean validBinaryString(String s) {
        return s.matches("[01]+");
    }

    /**
     * Get a string with the metrics of the executed memfile
     */
    public String toString() {
        String tempString = "";
        tempString += "\nMETRICS";
        tempString += "\n_________________________";
        tempString += "\n\t" + String.format("AVERAGED CPI:  %.2f", (0.0 + num_clocks) / instruction_trace.size());
        tempString += "\n\tNUM CLOCKS:    " + num_clocks;
        tempString += "\n\tNUM STALLS:    " + num_stalls;
        tempString += "\n\tLOAD OPS:      " + load_ops;
        tempString += "\n\tSTORE OPS:     " + store_ops;
        tempString += "\n\tALU OPS:       " + alu_ops;
        tempString += "\n\tBRANCH OPS:    " + branch_ops;
        tempString += "\n\tMISC OPS:      " + misc_ops;
        tempString += "\n";
        tempString += "\n";
        tempString += "\nINSTRUCTION TRACE" + " (" + instruction_trace.size() + " instructions executed)";
        tempString += "\n_________________________";
        for (Stage5State s : instruction_trace) {
            tempString += "\n\t" + s;
        }
        return tempString;
    }
}

